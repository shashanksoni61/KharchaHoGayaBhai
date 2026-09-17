package com.shashanksoni.kharchahogayabhai.core.normalization

import java.util.Locale

/**
 * Reduces the many ways sources write a merchant down to one comparable key.
 *
 * The same purchase reaches us as `Swiggy`, `SWIGGY`, `UPI/SWIGGY LIMITED/Payment`
 * or `swiggy@icici`, so matching on raw strings is hopeless.
 *
 * [canonicalKey] is for comparison and fingerprinting; [toDisplayName] is what
 * the UI shows.
 */
object MerchantNameNormalizer {

    /**
     * Plumbing words that appear in UPI narrations and statement descriptions but
     * say nothing about who was paid.
     */
    private val NOISE_TOKENS = setOf(
        "UPI", "IMPS", "NEFT", "RTGS", "POS", "ATM", "ECS", "ACH", "NACH",
        "PAYMENT", "PAYMENTS", "PAY", "PMT", "TXN", "TRANSACTION", "TRANSFER", "TRF",
        "REF", "REFNO", "RRN", "UTR", "NO", "ID",
        "DEBIT", "CREDIT", "DR", "CR", "PURCHASE", "MERCHANT", "ONLINE",
        "TO", "FROM", "VIA", "AT", "FOR", "THE", "AND",
        "PVT", "PVTLTD", "LTD", "LIMITED", "PRIVATE", "LLP", "INC", "CORP", "COMPANY",
        "COM", "CO", "IN", "INDIA", "TECHNOLOGIES", "TECHNOLOGY", "SERVICES", "SOLUTIONS",
        "BANK", "ACCOUNT", "AC", "ACCT",
    )

    /** Short tokens that read better fully capitalised, e.g. KFC, SBI, BSNL. */
    private const val ACRONYM_MAX_LENGTH = 3

    /** Long mixed-alphanumeric tokens are reference numbers, not names. */
    private const val IDENTIFIER_MIN_LENGTH = 8

    /**
     * A comparison key with punctuation, spacing and boilerplate removed, e.g.
     * `"UPI/Swiggy Instamart/Payment"` and `"SWIGGYINSTAMART"` both yield
     * `SWIGGYINSTAMART`. Null when nothing identifying is left.
     */
    fun canonicalKey(rawName: String?): String? {
        val tokens = meaningfulTokens(rawName)
        if (tokens.isEmpty()) return null
        return tokens.joinToString(separator = "")
    }

    /** A tidied, human-readable name, e.g. `"SWIGGY LIMITED"` becomes `"Swiggy"`. */
    fun toDisplayName(rawName: String?): String? {
        val tokens = meaningfulTokens(rawName)
        if (tokens.isEmpty()) return rawName?.trim()?.takeIf { it.isNotEmpty() }
        return tokens.joinToString(separator = " ") { token ->
            if (token.length <= ACRONYM_MAX_LENGTH) {
                token
            } else {
                token.first() + token.drop(1).lowercase(Locale.ROOT)
            }
        }
    }

    private fun meaningfulTokens(rawName: String?): List<String> {
        val raw = rawName?.trim().orEmpty()
        if (raw.isEmpty()) return emptyList()

        // For a VPA such as `swiggy@icici`, the handle after `@` is the payment
        // provider, not the merchant, so only the local part is interesting.
        val candidate = if (raw.contains('@')) raw.substringBefore('@') else raw

        return candidate
            .map { character -> if (character.isLetterOrDigit()) character else ' ' }
            .joinToString(separator = "")
            .uppercase(Locale.ROOT)
            .split(' ')
            .filter { it.isNotBlank() }
            .filterNot { it in NOISE_TOKENS }
            .filterNot { isIdentifier(it) }
    }

    private fun isIdentifier(token: String): Boolean =
        token.all { it.isDigit() } ||
            (token.length >= IDENTIFIER_MIN_LENGTH && token.any { it.isDigit() })
}
