package com.shashanksoni.kharchahogayabhai.core.normalization

import com.shashanksoni.kharchahogayabhai.core.deduplication.TransactionFingerprintFactory
import com.shashanksoni.kharchahogayabhai.domain.model.ParseStatus
import com.shashanksoni.kharchahogayabhai.domain.model.ParsedTransaction
import com.shashanksoni.kharchahogayabhai.domain.model.Transaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSourceRecord
import java.time.Clock
import java.time.ZoneId

/** A source-agnostic transaction plus the source record that produced it. */
data class NormalizedTransaction(
    val transaction: Transaction,
    val sourceRecord: TransactionSourceRecord,
)

/**
 * The single place where source-specific output becomes a storable transaction.
 *
 * Every parser funnels through here, which is what guarantees that an SMS and a
 * CSV row describing the same payment end up with identical references,
 * merchant keys and fingerprints. Parsers must not normalise anything
 * themselves.
 *
 * Invariants established here:
 * - [Transaction.amount] is always positive; direction lives in
 *   [com.shashanksoni.kharchahogayabhai.domain.model.TransactionType].
 * - [Transaction.accountIdentifier] holds at most the last four digits.
 * - Raw source text is kept only when parsing was incomplete, so the user can
 *   review it; clean imports retain nothing sensitive.
 */
class TransactionNormalizer(
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.systemUTC(),
) {

    fun normalize(parsed: ParsedTransaction): NormalizedTransaction {
        val now = clock.instant()
        val normalizedReference = TransactionReferenceNormalizer.normalize(parsed.referenceNumber)
        val accountIdentifier = AccountIdentifierNormalizer.normalize(parsed.accountIdentifier)
        val merchantName = MerchantNameNormalizer.toDisplayName(parsed.merchantName)

        val fingerprint = TransactionFingerprintFactory.create(
            TransactionFingerprintFactory.FingerprintInput(
                amount = parsed.amount,
                type = parsed.type,
                transactionDate = parsed.transactionDate,
                normalizedReference = normalizedReference,
                merchantName = parsed.merchantName,
                accountIdentifier = accountIdentifier,
                zone = zone,
            ),
        )

        val transaction = Transaction(
            amount = parsed.amount.absoluteValue,
            type = parsed.type,
            transactionDate = parsed.transactionDate,
            description = parsed.description?.trim()?.takeIf { it.isNotEmpty() },
            merchantName = merchantName,
            referenceNumber = parsed.referenceNumber?.trim()?.takeIf { it.isNotEmpty() },
            normalizedReference = normalizedReference,
            accountIdentifier = accountIdentifier,
            bankName = parsed.bankName?.trim()?.takeIf { it.isNotEmpty() },
            paymentMethod = parsed.paymentMethod,
            primarySource = parsed.source,
            fingerprint = fingerprint,
            parseStatus = parsed.parseStatus,
            isPromotional = parsed.isPromotional,
            createdAt = now,
            updatedAt = now,
        )

        val sourceRecord = TransactionSourceRecord(
            source = parsed.source,
            sourceIdentifier = parsed.sourceIdentifier,
            originLabel = parsed.originLabel,
            rawPayload = parsed.rawPayload.takeIf { parsed.parseStatus != ParseStatus.PARSED },
            importedAt = now,
        )

        return NormalizedTransaction(transaction, sourceRecord)
    }
}
