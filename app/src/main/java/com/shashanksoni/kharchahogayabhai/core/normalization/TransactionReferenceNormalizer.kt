package com.shashanksoni.kharchahogayabhai.core.normalization

import java.util.Locale

/**
 * Extracts the bank reference (UPI ref, UTR, RRN, cheque number) from however a
 * source happens to label it, and returns it in one canonical form.
 *
 * `"UPI Ref: 1234 5678 9012"`, `"Transaction ID 123456789012"` and
 * `"UPI/123456789012/Swiggy"` all reduce to `123456789012`, which is what makes
 * the strongest deduplication check possible across sources.
 *
 * A reference is only returned when it looks trustworthy. Short or digit-free
 * fragments are dropped rather than risking two unrelated transactions being
 * declared identical.
 */
object TransactionReferenceNormalizer {

    private const val MIN_LENGTH = 6
    private const val MIN_DIGITS = 4

    /** Words used to label a reference; never part of the reference itself. */
    private val LABEL_TOKENS = setOf(
        "UPI", "REF", "REFNO", "REFERENCE", "NO", "NUM", "NUMBER",
        "TXN", "TRANSACTION", "TRANS", "ID", "IDNO",
        "UTR", "RRN", "IMPS", "NEFT", "RTGS", "NPCI", "SEQ", "CHQ", "CHEQUE",
        "P2M", "P2A", "P2P",
    )

    /**
     * Returns the canonical reference, or null when [rawReference] holds nothing
     * strong enough to identify a transaction by.
     */
    fun normalize(rawReference: String?): String? {
        val raw = rawReference?.trim().orEmpty()
        if (raw.isEmpty()) return null

        val tokens = raw
            .map { character -> if (character.isLetterOrDigit()) character else ' ' }
            .joinToString(separator = "")
            .uppercase(Locale.ROOT)
            .split(' ')
            .filter { it.isNotBlank() }
            .filterNot { it in LABEL_TOKENS }

        // Banks split long references with spaces or dashes; if every remaining
        // fragment is numeric, they are pieces of one number.
        val candidate = if (tokens.size > 1 && tokens.all { token -> token.all(Char::isDigit) }) {
            tokens.joinToString(separator = "")
        } else {
            tokens.filter { token -> token.any(Char::isDigit) }.maxByOrNull { it.length }
        }

        return candidate?.takeIf(::isTrustworthy)
    }

    private fun isTrustworthy(reference: String): Boolean =
        reference.length >= MIN_LENGTH && reference.count(Char::isDigit) >= MIN_DIGITS
}
