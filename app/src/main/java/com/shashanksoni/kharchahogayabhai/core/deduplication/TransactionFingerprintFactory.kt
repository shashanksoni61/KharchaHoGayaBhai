package com.shashanksoni.kharchahogayabhai.core.deduplication

import com.shashanksoni.kharchahogayabhai.core.common.Sha256
import com.shashanksoni.kharchahogayabhai.core.normalization.AccountIdentifierNormalizer
import com.shashanksoni.kharchahogayabhai.core.normalization.MerchantNameNormalizer
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Builds the deterministic identity of a transaction, so the same real-world
 * payment produces the same fingerprint no matter which source described it.
 *
 * Two strategies, picked by how much we can trust the input:
 *
 * 1. [Strategy.REFERENCE] — a bank reference survived normalisation. Reference
 *    plus amount plus direction is used rather than the reference alone, because
 *    some banks print one reference on both legs of a transfer.
 * 2. [Strategy.FIELDS] — no usable reference, so identity falls back to amount,
 *    calendar day, merchant key, account tail and direction.
 *
 * Deliberate choices worth knowing about:
 * - The date is bucketed to a **calendar day in [FingerprintInput.zone]**, not to
 *   an exact timestamp. SMS carries a time, statements usually carry only a date,
 *   so anything finer would never match across sources.
 * - Day bucketing means a source that reports the same payment one day late gets
 *   a different fingerprint. Fingerprinting is exact-match only by design;
 *   near-miss matching across a date window is the deduplication engine's job.
 */
object TransactionFingerprintFactory {

    private const val VERSION = "v1"
    private val DAY_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

    enum class Strategy { REFERENCE, FIELDS }

    data class FingerprintInput(
        val amount: Money,
        val type: TransactionType,
        val transactionDate: Instant,
        val normalizedReference: String?,
        val merchantName: String?,
        val accountIdentifier: String?,
        val zone: ZoneId = ZoneId.systemDefault(),
    )

    fun create(input: FingerprintInput): String = "$VERSION:${Sha256.hexOf(payloadOf(input))}"

    fun strategyOf(input: FingerprintInput): Strategy =
        if (input.normalizedReference.isNullOrBlank()) Strategy.FIELDS else Strategy.REFERENCE

    /** Exposed for tests and debugging; never logged in release builds. */
    internal fun payloadOf(input: FingerprintInput): String {
        val amountPart = "${input.amount.currencyCode}:${input.amount.absoluteValue.minorUnits}"
        return when (strategyOf(input)) {
            Strategy.REFERENCE -> listOf(
                VERSION,
                Strategy.REFERENCE.name,
                amountPart,
                input.type.name,
                input.normalizedReference.orEmpty(),
            )

            Strategy.FIELDS -> listOf(
                VERSION,
                Strategy.FIELDS.name,
                amountPart,
                input.type.name,
                DAY_FORMATTER.format(input.transactionDate.atZone(input.zone).toLocalDate()),
                MerchantNameNormalizer.canonicalKey(input.merchantName).orEmpty(),
                AccountIdentifierNormalizer.normalize(input.accountIdentifier).orEmpty(),
            )
        }.joinToString(separator = "|")
    }
}
