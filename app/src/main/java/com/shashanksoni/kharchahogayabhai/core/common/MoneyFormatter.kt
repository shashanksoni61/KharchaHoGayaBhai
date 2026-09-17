package com.shashanksoni.kharchahogayabhai.core.common

import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Formats [Money] for display.
 *
 * Rupee amounts are grouped the Indian way (`₹1,23,456`), and paise are hidden
 * for round amounts so lists stay scannable.
 */
object MoneyFormatter {

    private val IndiaLocale: Locale = Locale.Builder().setLanguage("en").setRegion("IN").build()

    /** e.g. `₹540`, `₹1,240.50`, `₹1,23,456`. */
    fun format(money: Money, alwaysShowFraction: Boolean = false): String {
        val showFraction = alwaysShowFraction || hasFractionalPart(money)
        return currencyFormatterFor(money, showFraction).format(money.toBigDecimal())
    }

    /** e.g. `- ₹540` for a debit and `+ ₹95,000` for a credit. */
    fun formatSigned(money: Money, type: TransactionType): String {
        val sign = if (type == TransactionType.DEBIT) '\u2212' else '+' // minus sign, not hyphen
        return "$sign ${format(money.absoluteValue)}"
    }

    /** Short form for chart axes and tight labels, e.g. `₹8.4K`, `₹1.2L`, `₹3.4Cr`. */
    fun formatCompact(money: Money): String {
        val symbol = symbolOf(money.currencyCode)
        val units = money.absoluteValue.toBigDecimal().toDouble()
        val (scaled, suffix) = when {
            units >= CRORE -> units / CRORE to "Cr"
            units >= LAKH -> units / LAKH to "L"
            units >= THOUSAND -> units / THOUSAND to "K"
            else -> return "$symbol${DecimalFormat("#,##0").format(units)}"
        }
        val pattern = if (scaled >= 10) "#,##0" else "#,##0.#"
        return "$symbol${DecimalFormat(pattern).format(scaled)}$suffix"
    }

    private fun hasFractionalPart(money: Money): Boolean {
        if (money.fractionDigits == 0) return false
        var divisor = 1L
        repeat(money.fractionDigits) { divisor *= 10 }
        return money.minorUnits % divisor != 0L
    }

    /**
     * A fresh formatter per call: [NumberFormat] is mutable and not thread-safe,
     * and formatting a handful of amounts per frame is not worth caching.
     */
    private fun currencyFormatterFor(money: Money, showFraction: Boolean): NumberFormat {
        val locale = if (money.currencyCode == Money.DEFAULT_CURRENCY_CODE) {
            IndiaLocale
        } else {
            Locale.getDefault()
        }
        return NumberFormat.getCurrencyInstance(locale).apply {
            runCatching { currency = Currency.getInstance(money.currencyCode) }
            maximumFractionDigits = if (showFraction) money.fractionDigits else 0
            minimumFractionDigits = if (showFraction) money.fractionDigits else 0
        }
    }

    private fun symbolOf(currencyCode: String): String = runCatching {
        Currency.getInstance(currencyCode).getSymbol(IndiaLocale)
    }.getOrDefault(currencyCode)

    private const val THOUSAND = 1_000.0
    private const val LAKH = 100_000.0
    private const val CRORE = 10_000_000.0
}
