package com.shashanksoni.kharchahogayabhai.core.normalization

/**
 * Reduces an account or card reference to its last four digits.
 *
 * `"A/c XX1234"`, `"****1234"` and `"50100123451234"` all become `"1234"`, which
 * is the only part every source agrees on. Storing just four digits is also the
 * least sensitive thing we can keep.
 *
 * Four digits are not unique, so this is a supporting signal for deduplication,
 * never a decisive one on its own.
 */
object AccountIdentifierNormalizer {

    private const val VISIBLE_DIGITS = 4
    private const val MASK_CHARACTER = '\u2022' // bullet

    fun normalize(rawIdentifier: String?): String? {
        val digits = rawIdentifier?.filter(Char::isDigit).orEmpty()
        if (digits.length < VISIBLE_DIGITS) return null
        return digits.takeLast(VISIBLE_DIGITS)
    }

    /** Display form for an already normalised identifier, e.g. `"••••1234"`. */
    fun toMaskedDisplay(normalizedIdentifier: String?): String? {
        val identifier = normalizedIdentifier?.takeIf { it.isNotBlank() } ?: return null
        return "${MASK_CHARACTER.toString().repeat(VISIBLE_DIGITS)}$identifier"
    }
}
