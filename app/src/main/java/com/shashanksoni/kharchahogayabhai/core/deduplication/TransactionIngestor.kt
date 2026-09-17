package com.shashanksoni.kharchahogayabhai.core.deduplication

import androidx.room.withTransaction
import com.shashanksoni.kharchahogayabhai.core.database.KharchaDatabase
import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionDao
import com.shashanksoni.kharchahogayabhai.core.database.dao.TransactionSourceDao
import com.shashanksoni.kharchahogayabhai.core.normalization.NormalizedTransaction
import com.shashanksoni.kharchahogayabhai.core.normalization.TransactionNormalizer
import com.shashanksoni.kharchahogayabhai.data.local.mapper.toDomain
import com.shashanksoni.kharchahogayabhai.data.local.mapper.toEntity
import com.shashanksoni.kharchahogayabhai.domain.model.IngestOutcome
import com.shashanksoni.kharchahogayabhai.domain.model.ParseStatus
import com.shashanksoni.kharchahogayabhai.domain.model.ParsedTransaction
import com.shashanksoni.kharchahogayabhai.domain.model.Transaction

/**
 * Writes parsed transactions into the local store with deduplication.
 *
 * Matching order:
 * 1. Exact same source row already ingested → skip
 * 2. Same fingerprint → merge
 * 3. Same normalised bank reference + amount + type → merge
 * 4. Otherwise create a new transaction
 *
 * Merges keep provenance via [com.shashanksoni.kharchahogayabhai.domain.model.TransactionSourceRecord].
 */
class TransactionIngestor(
    private val database: KharchaDatabase,
    private val transactionDao: TransactionDao,
    private val transactionSourceDao: TransactionSourceDao,
    private val normalizer: TransactionNormalizer,
    private val merger: TransactionMerger,
) {

    suspend fun ingest(parsedTransactions: List<ParsedTransaction>): List<IngestOutcome> {
        return database.withTransaction {
            parsedTransactions.map { parsed -> ingestOne(parsed) }
        }
    }

    private suspend fun ingestOne(parsed: ParsedTransaction): IngestOutcome {
        if (parsed.parseStatus == ParseStatus.FAILED && parsed.amount.isZero) {
            return IngestOutcome.FAILED
        }

        val sourceId = parsed.sourceIdentifier
        if (sourceId != null) {
            val existingSource = transactionSourceDao.findBySourceIdentifier(parsed.source, sourceId)
            if (existingSource != null) {
                return IngestOutcome.DUPLICATE_SKIPPED
            }
        }

        val normalized = normalizer.normalize(parsed)
        if (parsed.parseStatus == ParseStatus.FAILED) {
            // Still store failed rows so the user can review them later via sources.
            return insertNew(normalized)
        }

        findDuplicate(normalized.transaction)?.let { existing ->
            return mergeIntoExisting(existing, normalized)
        }

        return insertNew(normalized)
    }

    private suspend fun findDuplicate(incoming: Transaction): Transaction? {
        transactionDao.findByFingerprint(incoming.fingerprint)?.let { return it.toDomain() }

        val reference = incoming.normalizedReference ?: return null
        val candidates = transactionDao.findByNormalizedReference(reference)
        return candidates
            .map { it.toDomain() }
            .firstOrNull { candidate ->
                candidate.amount.minorUnits == incoming.amount.minorUnits &&
                    candidate.amount.currencyCode == incoming.amount.currencyCode &&
                    candidate.type == incoming.type
            }
    }

    private suspend fun mergeIntoExisting(
        existing: Transaction,
        incoming: NormalizedTransaction,
    ): IngestOutcome {
        val alreadyLinked = incoming.sourceRecord.sourceIdentifier?.let { sourceId ->
            transactionSourceDao.findBySourceIdentifier(incoming.sourceRecord.source, sourceId) != null
        } == true
        if (alreadyLinked) return IngestOutcome.DUPLICATE_SKIPPED

        val merged = merger.merge(existing, incoming.transaction)
        transactionDao.updateTransaction(merged.toEntity())
        transactionSourceDao.insertSourceRecords(
            listOf(incoming.sourceRecord.toEntity(existing.id)),
        )
        return IngestOutcome.MERGED
    }

    private suspend fun insertNew(normalized: NormalizedTransaction): IngestOutcome {
        val id = transactionDao.insertTransaction(normalized.transaction.toEntity())
        transactionSourceDao.insertSourceRecords(
            listOf(normalized.sourceRecord.toEntity(id)),
        )
        return if (normalized.transaction.parseStatus == ParseStatus.FAILED) {
            IngestOutcome.FAILED
        } else {
            IngestOutcome.CREATED
        }
    }
}
