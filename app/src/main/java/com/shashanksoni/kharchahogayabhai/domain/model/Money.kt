package com.shashanksoni.kharchahogayabhai.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

/**
 * A monetary value held as an integral number of minor units (paise for INR).
 *
 * Money never touches [Double]/[Float]: rounding drift is unacceptable in a
 * ledger, and integral minor units also give us stable equality, which the
 * deduplication engine relies on when comparing amounts across sources.
 */
data class Money(
    val minorUnits: Long,
    val currencyCode: String = DEFAULT_CURRENCY_CODE,
) : Comparable<Money> {

    init {
        require(currencyCode.length == 3) { "Expected an ISO-4217 currency code, got '$currencyCode'" }
    }

    val fractionDigits: Int get() = fractionDigitsOf(currencyCode)

    val isZero: Boolean get() = minorUnits == 0L

    val absoluteValue: Money get() = if (minorUnits < 0) copy(minorUnits = -minorUnits) else this

    fun toBigDecimal(): BigDecimal = BigDecimal.valueOf(minorUnits, fractionDigits)

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = minorUnits + other.minorUnits)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = minorUnits - other.minorUnits)
    }

    /** Share of [total] in the 0f..1f range; zero when [total] is zero. */
    fun shareOf(total: Money): Float {
        requireSameCurrency(total)
        if (total.minorUnits == 0L) return 0f
        return (minorUnits.toDouble() / total.minorUnits.toDouble()).toFloat()
    }

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    private fun requireSameCurrency(other: Money) {
        require(currencyCode == other.currencyCode) {
            "Cannot combine $currencyCode with ${other.currencyCode}"
        }
    }

    companion object {
        const val DEFAULT_CURRENCY_CODE = "INR"

        private val fractionDigitCache = mutableMapOf<String, Int>()

        fun zero(currencyCode: String = DEFAULT_CURRENCY_CODE): Money = Money(0L, currencyCode)

        fun fromMinorUnits(minorUnits: Long, currencyCode: String = DEFAULT_CURRENCY_CODE): Money =
            Money(minorUnits, currencyCode)

        /** Builds money from a major-unit amount such as `540.50`. */
        fun fromMajorUnits(
            amount: BigDecimal,
            currencyCode: String = DEFAULT_CURRENCY_CODE,
        ): Money = Money(
            minorUnits = amount
                .setScale(fractionDigitsOf(currencyCode), RoundingMode.HALF_EVEN)
                .movePointRight(fractionDigitsOf(currencyCode))
                .longValueExact(),
            currencyCode = currencyCode,
        )

        /**
         * Parses a major-unit amount as it typically appears in statements, e.g.
         * `"1,299.00"`, `"₹540"` or `"540.50 Dr"`. Returns null when no number is found,
         * so parsers can flag the row instead of inventing a value.
         */
        fun parseMajorUnitsOrNull(
            rawAmount: String,
            currencyCode: String = DEFAULT_CURRENCY_CODE,
        ): Money? {
            val digitsAndSeparators = rawAmount.filter { it.isDigit() || it == '.' || it == '-' }
                .removeSuffix(".")
            if (digitsAndSeparators.none { it.isDigit() }) return null
            val parsed = digitsAndSeparators.toBigDecimalOrNull() ?: return null
            return fromMajorUnits(parsed, currencyCode)
        }

        fun sum(values: Iterable<Money>, currencyCode: String = DEFAULT_CURRENCY_CODE): Money =
            values.fold(zero(currencyCode)) { total, value -> total + value }

        private fun fractionDigitsOf(currencyCode: String): Int =
            fractionDigitCache.getOrPut(currencyCode) {
                runCatching { Currency.getInstance(currencyCode).defaultFractionDigits }
                    .getOrDefault(2)
                    .coerceAtLeast(0)
            }
    }
}
