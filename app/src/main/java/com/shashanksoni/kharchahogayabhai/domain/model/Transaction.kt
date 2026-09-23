package com.shashanksoni.kharchahogayabhai.domain.model

import java.time.Instant

/**
 * The one transaction model every source eventually produces.
 *
 * SMS, CSV, PDF and manual entry all funnel into this shape, so reporting and UI
 * never need to know where a transaction came from.
 *
 * Field notes:
 * - [referenceNumber] is the human-readable reference printed by the bank (UPI
 *   ref, UTR, RRN, cheque number). [normalizedReference] is its canonical form
 *   and is what deduplication compares, because the same reference is formatted
 *   differently by every source.
 * - [primarySource] records which source first created this row. The full set of
 *   contributing sources lives in [TransactionSourceRecord], never overwritten
 *   when a duplicate is merged in.
 * - [fingerprint] is the deterministic identity used when no trustworthy
 *   reference exists. It is unique in storage.
 */
data class Transaction(
    val id: Long = UNSAVED_ID,
    val amount: Money,
    val type: TransactionType,
    val transactionDate: Instant,
    val description: String? = null,
    val merchantName: String? = null,
    val referenceNumber: String? = null,
    val normalizedReference: String? = null,
    /** Last four digits of the account or card, e.g. `"1234"`; the UI adds masking. */
    val accountIdentifier: String? = null,
    val bankName: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.UNKNOWN,
    val categoryId: Long? = null,
    val primarySource: TransactionSource,
    val fingerprint: String,
    val parseStatus: ParseStatus = ParseStatus.PARSED,
    /**
     * Marketing / wallet-offer SMS kept in the list so the user can see them,
     * but excluded from money-in / money-out totals.
     */
    val isPromotional: Boolean = false,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    /** What to show when a source gave us no merchant, e.g. a bare ATM withdrawal. */
    val displayTitle: String
        get() = merchantName?.takeIf { it.isNotBlank() }
            ?: description?.takeIf { it.isNotBlank() }
            ?: bankName?.takeIf { it.isNotBlank() }
            ?: UNTITLED_TRANSACTION

    val isDebit: Boolean get() = type == TransactionType.DEBIT

    val countsTowardTotals: Boolean get() = !isPromotional

    companion object {
        const val UNSAVED_ID = 0L
        const val UNTITLED_TRANSACTION = "Unlabelled transaction"
    }
}
