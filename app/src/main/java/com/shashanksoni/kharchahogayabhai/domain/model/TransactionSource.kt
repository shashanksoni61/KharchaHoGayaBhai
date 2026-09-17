package com.shashanksoni.kharchahogayabhai.domain.model

/**
 * Where a piece of transaction data came from.
 *
 * A single transaction can be backed by several sources; see
 * [TransactionSourceRecord]. New sources (email, bank API, credit card
 * statements) are added here without touching storage or reporting code.
 */
enum class TransactionSource {
    SMS,
    CSV,
    PDF,
    MANUAL,
}
