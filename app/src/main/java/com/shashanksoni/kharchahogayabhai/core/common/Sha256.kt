package com.shashanksoni.kharchahogayabhai.core.common

import java.security.MessageDigest

/** SHA-256 hex digests, used to build deterministic transaction fingerprints. */
object Sha256 {

    fun hexOf(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return buildString(digest.size * 2) {
            digest.forEach { byte ->
                val unsigned = byte.toInt() and 0xFF
                append(HEX_DIGITS[unsigned ushr 4])
                append(HEX_DIGITS[unsigned and 0x0F])
            }
        }
    }

    private const val HEX_DIGITS = "0123456789abcdef"
}
