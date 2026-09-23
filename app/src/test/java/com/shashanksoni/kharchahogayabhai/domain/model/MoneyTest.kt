package com.shashanksoni.kharchahogayabhai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun `major units convert to minor units`() {
        assertEquals(54000L, Money.fromMajorUnits(BigDecimal("540.00")).minorUnits)
        assertEquals(54050L, Money.fromMajorUnits(BigDecimal("540.50")).minorUnits)
        assertEquals(54000L, Money.fromMajorUnits(BigDecimal("540")).minorUnits)
    }

    @Test
    fun `sub-paise input is rounded rather than truncated`() {
        assertEquals(54058L, Money.fromMajorUnits(BigDecimal("540.575")).minorUnits)
    }

    @Test
    fun `repeated addition stays exact`() {
        val tenPaise = Money.fromMajorUnits(BigDecimal("0.10"))
        val total = (1..10).fold(Money.zero()) { sum, _ -> sum + tenPaise }
        assertEquals(Money.fromMajorUnits(BigDecimal("1.00")), total)
        assertEquals(BigDecimal("1.00"), total.toBigDecimal())
    }

    @Test
    fun `statement amounts are parsed from their printed form`() {
        assertEquals(129900L, Money.parseMajorUnitsOrNull("1,299.00")?.minorUnits)
        assertEquals(54000L, Money.parseMajorUnitsOrNull("₹540")?.minorUnits)
        assertEquals(54050L, Money.parseMajorUnitsOrNull("540.50 Dr")?.minorUnits)
        assertEquals(54000L, Money.parseMajorUnitsOrNull("INR 540.")?.minorUnits)
    }

    @Test
    fun `amounts without digits are rejected instead of defaulting to zero`() {
        assertNull(Money.parseMajorUnitsOrNull("-"))
        assertNull(Money.parseMajorUnitsOrNull(""))
        assertNull(Money.parseMajorUnitsOrNull("NA"))
    }

    @Test
    fun `share of a total is a fraction and zero-safe`() {
        val total = Money.fromMajorUnits(BigDecimal("1000.00"))
        assertEquals(0.25f, Money.fromMajorUnits(BigDecimal("250.00")).shareOf(total), 0.0001f)
        assertEquals(0f, Money.fromMajorUnits(BigDecimal("250.00")).shareOf(Money.zero()), 0.0001f)
    }

    @Test
    fun `summing a month keeps the default currency and skips others`() {
        val rupees = Money.fromMajorUnits(BigDecimal("100.00"), "INR")
        val dollars = Money.fromMajorUnits(BigDecimal("25.00"), "USD")
        assertEquals(rupees, Money.sum(listOf(rupees, dollars)))
        assertEquals(dollars, Money.sum(listOf(dollars)))
        assertEquals(
            Money.fromMajorUnits(BigDecimal("-25.00"), "USD"),
            Money.net(Money.zero(), dollars),
        )
    }

    @Test
    fun `mixing currencies is refused`() {
        val rupees = Money.fromMajorUnits(BigDecimal("100.00"), "INR")
        val dollars = Money.fromMajorUnits(BigDecimal("100.00"), "USD")
        val failure = runCatching { rupees + dollars }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `currencies without minor units are handled`() {
        val yen = Money.fromMajorUnits(BigDecimal("500"), "JPY")
        assertEquals(500L, yen.minorUnits)
        assertEquals(0, yen.fractionDigits)
    }
}
