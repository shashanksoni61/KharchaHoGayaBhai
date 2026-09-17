package com.shashanksoni.kharchahogayabhai.domain.model

import java.time.Instant

/**
 * What a source parser produces: transaction fields exactly as read from one
 * source, before normalisation, fingerprinting and deduplication.
 *
 * It is deliberately separate from [Transaction]. A parsed item has no database
 * id, no fingerprint and no category, and it may be incomplete (see
 * [parseStatus]). Keeping the two apart means an SMS parser never has to know
 * how identity or storage works.
 *
 * A parser that cannot make sense of its input still emits a
 * [ParseStatus.FAILED] item carrying [rawPayload], so the import screen can ask
 * the user what to do instead of silently dropping the row.
 */
data class ParsedTransaction(
    val amount: Money,
    val type: TransactionType,
    val transactionDate: Instant,
    val description: String? = null,
    val merchantName: String? = null,
    val referenceNumber: String? = null,
    val accountIdentifier: String? = null,
    val bankName: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.UNKNOWN,
    val source: TransactionSource,
    val sourceIdentifier: String? = null,
    val originLabel: String? = null,
    val parseStatus: ParseStatus = ParseStatus.PARSED,
    val rawPayload: String? = null,
)
