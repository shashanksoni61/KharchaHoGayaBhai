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
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource

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
                return reclassifyExisting(existingSource.transactionId, parsed)
            }
        }

        val normalized = normalizer.normalize(parsed)
        if (parsed.parseStatus == ParseStatus.FAILED) {
            // Still store failed rows so the user can review them later via sources.
            return insertNew(normalized)
        }

        findDuplicate(normalized)?.let { existing ->
            return mergeIntoExisting(existing, normalized)
        }

        return insertNew(normalized)
    }

    private suspend fun findDuplicate(incoming: NormalizedTransaction): Transaction? {
        val transaction = incoming.transaction
        transactionDao.findByFingerprint(transaction.fingerprint)?.let { entity ->
            val existing = entity.toDomain()
            if (isDistinctSmsWithSameFieldFingerprint(incoming, existing)) {
                return null
            }
            return existing
        }

        val reference = transaction.normalizedReference ?: return null
        val candidates = transactionDao.findByNormalizedReference(reference)
        return candidates
            .map { it.toDomain() }
            .firstOrNull { candidate ->
                candidate.amount.minorUnits == transaction.amount.minorUnits &&
                    candidate.amount.currencyCode == transaction.amount.currencyCode &&
                    candidate.type == transaction.type
            }
    }

    /**
     * Field fingerprints ignore time and SMS id, so two Axis alerts on the same
     * day for the same amount used to collapse into one row. Distinct inbox ids
     * are different payments unless they share a bank UTR.
     */
    private suspend fun isDistinctSmsWithSameFieldFingerprint(
        incoming: NormalizedTransaction,
        existing: Transaction,
    ): Boolean {
        if (incoming.sourceRecord.source != TransactionSource.SMS) return false
        val incomingKey = incoming.sourceRecord.sourceIdentifier ?: return false
        if (!incoming.transaction.normalizedReference.isNullOrBlank() &&
            incoming.transaction.normalizedReference == existing.normalizedReference
        ) {
            return false
        }
        val existingSmsKeys = transactionSourceDao.findSourcesOf(existing.id)
            .filter { it.source == TransactionSource.SMS }
            .mapNotNull { it.sourceIdentifier }
        return existingSmsKeys.isNotEmpty() && incomingKey !in existingSmsKeys
    }

    /**
     * The same SMS id is not ingested twice, but a later parser can flip an
     * older row to promotional (limit / offer / due alerts that used to look
     * like payments).
     */
    private suspend fun reclassifyExisting(
        transactionId: Long,
        parsed: ParsedTransaction,
    ): IngestOutcome {
        val existing = transactionDao.findById(transactionId)?.toDomain()
            ?: return IngestOutcome.DUPLICATE_SKIPPED
        val incoming = normalizer.normalize(parsed).transaction
        if (existing.isPromotional == incoming.isPromotional) {
            return IngestOutcome.DUPLICATE_SKIPPED
        }
        transactionDao.updateTransaction(
            existing.copy(
                isPromotional = incoming.isPromotional,
                updatedAt = incoming.updatedAt,
            ).toEntity(),
        )
        return IngestOutcome.MERGED
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
        var transaction = normalized.transaction
        if (transactionDao.findByFingerprint(transaction.fingerprint) != null) {
            val extra = normalized.sourceRecord.sourceIdentifier ?: "extra"
            transaction = transaction.copy(fingerprint = "${transaction.fingerprint}+$extra")
        }
        val id = transactionDao.insertTransaction(transaction.toEntity())
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
