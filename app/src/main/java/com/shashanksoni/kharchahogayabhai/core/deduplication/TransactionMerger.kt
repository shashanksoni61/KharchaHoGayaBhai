package com.shashanksoni.kharchahogayabhai.core.deduplication

import com.shashanksoni.kharchahogayabhai.domain.model.ParseStatus
import com.shashanksoni.kharchahogayabhai.domain.model.PaymentMethod
import com.shashanksoni.kharchahogayabhai.domain.model.Transaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Folds a duplicate into the transaction that already exists, keeping the union
 * of what both sources knew.
 *
 * Detecting a duplicate and merging it are separate steps on purpose: detection
 * answers "have we seen this payment?", merging answers "what does this extra
 * source add?". Discarding the second copy would throw away real information —
 * an SMS knows the payment method, a CSV knows the bank and account, a PDF often
 * has the cleanest date.
 *
 * Only gaps are filled. A value the user can edit (category, notes) is never
 * overwritten by an import, and identity fields (id, fingerprint, amount,
 * direction, creating source) always stay with the existing row.
 */
class TransactionMerger(
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.systemUTC(),
) {

    fun merge(existing: Transaction, incoming: Transaction): Transaction = existing.copy(
        transactionDate = preferMoreSpecificTimestamp(existing, incoming),
        description = existing.description ?: incoming.description,
        merchantName = existing.merchantName ?: incoming.merchantName,
        referenceNumber = existing.referenceNumber ?: incoming.referenceNumber,
        normalizedReference = existing.normalizedReference ?: incoming.normalizedReference,
        accountIdentifier = existing.accountIdentifier ?: incoming.accountIdentifier,
        bankName = existing.bankName ?: incoming.bankName,
        paymentMethod = preferKnownPaymentMethod(existing.paymentMethod, incoming.paymentMethod),
        categoryId = existing.categoryId ?: incoming.categoryId,
        parseStatus = preferCompleteParseStatus(existing.parseStatus, incoming.parseStatus),
        isPromotional = mergePromotional(existing, incoming),
        isIgnored = existing.isIgnored,
        notes = existing.notes ?: incoming.notes,
        updatedAt = clock.instant(),
    )

    /**
     * A statement gives a date and no time, which lands on local midnight; an SMS
     * gives the real time. Prefer whichever timestamp actually carries a time of
     * day, otherwise keep the existing one.
     */
    private fun preferMoreSpecificTimestamp(existing: Transaction, incoming: Transaction): Instant {
        val existingHasTime = hasTimeOfDay(existing)
        val incomingHasTime = hasTimeOfDay(incoming)
        return if (!existingHasTime && incomingHasTime) {
            incoming.transactionDate
        } else {
            existing.transactionDate
        }
    }

    private fun hasTimeOfDay(transaction: Transaction): Boolean =
        transaction.transactionDate.atZone(zone).toLocalTime() != LocalTime.MIDNIGHT

    private fun preferKnownPaymentMethod(
        existing: PaymentMethod,
        incoming: PaymentMethod,
    ): PaymentMethod = if (existing == PaymentMethod.UNKNOWN) incoming else existing

    /**
     * A full SMS rescan must be able to flip an older row to promotional.
     * A later CSV/PDF of the same payment means real money moved, so it counts.
     */
    private fun mergePromotional(existing: Transaction, incoming: Transaction): Boolean =
        if (incoming.primarySource == TransactionSource.SMS) {
            incoming.isPromotional
        } else {
            existing.isPromotional && incoming.isPromotional
        }

    /** A second source can complete a partially parsed transaction. */
    private fun preferCompleteParseStatus(
        existing: ParseStatus,
        incoming: ParseStatus,
    ): ParseStatus = listOf(existing, incoming).minBy { status ->
        when (status) {
            ParseStatus.PARSED -> 0
            ParseStatus.PARTIALLY_PARSED -> 1
            ParseStatus.FAILED -> 2
        }
    }
}
