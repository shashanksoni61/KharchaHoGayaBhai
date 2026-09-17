package com.shashanksoni.kharchahogayabhai.core.deduplication

import com.shashanksoni.kharchahogayabhai.domain.model.DefaultCategories
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.ParseStatus
import com.shashanksoni.kharchahogayabhai.domain.model.PaymentMethod
import com.shashanksoni.kharchahogayabhai.domain.model.Transaction
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Merging a duplicate must add information, never remove it, and never overwrite
 * what the user chose.
 */
class TransactionMergerTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private val mergedAt: Instant = Instant.parse("2026-09-18T04:30:00Z")
    private val merger = TransactionMerger(zone = zone, clock = Clock.fixed(mergedAt, zone))

    private val paymentDate: LocalDate = LocalDate.of(2026, 9, 17)
    private val smsTimestamp: Instant = paymentDate.atTime(LocalTime.of(15, 14)).atZone(zone).toInstant()
    private val statementTimestamp: Instant = paymentDate.atStartOfDay(zone).toInstant()

    @Test
    fun `a CSV duplicate fills in the bank and account the SMS never had`() {
        val fromSms = transaction(
            id = 7L,
            source = TransactionSource.SMS,
            merchantName = "Swiggy",
            paymentMethod = PaymentMethod.UPI,
            transactionDate = smsTimestamp,
            bankName = null,
            accountIdentifier = null,
        )
        val fromCsv = transaction(
            source = TransactionSource.CSV,
            merchantName = "SWIGGY",
            paymentMethod = PaymentMethod.UNKNOWN,
            transactionDate = statementTimestamp,
            bankName = "HDFC Bank",
            accountIdentifier = "1234",
        )

        val merged = merger.merge(existing = fromSms, incoming = fromCsv)

        assertEquals("HDFC Bank", merged.bankName)
        assertEquals("1234", merged.accountIdentifier)
        assertEquals(PaymentMethod.UPI, merged.paymentMethod)
        assertEquals("Swiggy", merged.merchantName)
        assertEquals(7L, merged.id)
        assertEquals(TransactionSource.SMS, merged.primarySource)
        assertEquals(mergedAt, merged.updatedAt)
    }

    @Test
    fun `the timestamp that carries a time of day wins`() {
        val fromStatement = transaction(
            source = TransactionSource.CSV,
            transactionDate = statementTimestamp,
        )
        val fromSms = transaction(
            source = TransactionSource.SMS,
            transactionDate = smsTimestamp,
        )

        assertEquals(smsTimestamp, merger.merge(fromStatement, fromSms).transactionDate)
        assertEquals(smsTimestamp, merger.merge(fromSms, fromStatement).transactionDate)
    }

    @Test
    fun `an import never overwrites the category the user picked`() {
        val categorisedByUser = transaction(categoryId = DefaultCategories.FOOD)
        val fromImport = transaction(categoryId = DefaultCategories.OTHER)

        assertEquals(
            DefaultCategories.FOOD,
            merger.merge(categorisedByUser, fromImport).categoryId,
        )
    }

    @Test
    fun `an uncategorised transaction accepts a category from the duplicate`() {
        val uncategorised = transaction(categoryId = null)
        val categorised = transaction(categoryId = DefaultCategories.FOOD)

        assertEquals(DefaultCategories.FOOD, merger.merge(uncategorised, categorised).categoryId)
    }

    @Test
    fun `a second source can complete a partially parsed transaction`() {
        val partial = transaction(parseStatus = ParseStatus.PARTIALLY_PARSED)
        val complete = transaction(parseStatus = ParseStatus.PARSED)

        assertEquals(ParseStatus.PARSED, merger.merge(partial, complete).parseStatus)
        assertEquals(ParseStatus.PARSED, merger.merge(complete, partial).parseStatus)
    }

    @Test
    fun `a missing reference number is picked up from the duplicate`() {
        val withoutReference = transaction(referenceNumber = null, normalizedReference = null)
        val withReference = transaction(
            referenceNumber = "UPI Ref 123456789012",
            normalizedReference = "123456789012",
        )

        val merged = merger.merge(withoutReference, withReference)

        assertEquals("UPI Ref 123456789012", merged.referenceNumber)
        assertEquals("123456789012", merged.normalizedReference)
    }

    private fun transaction(
        id: Long = 1L,
        source: TransactionSource = TransactionSource.SMS,
        merchantName: String? = "Swiggy",
        paymentMethod: PaymentMethod = PaymentMethod.UPI,
        transactionDate: Instant = smsTimestamp,
        bankName: String? = "HDFC Bank",
        accountIdentifier: String? = "1234",
        categoryId: Long? = null,
        parseStatus: ParseStatus = ParseStatus.PARSED,
        referenceNumber: String? = "UPI Ref 123456789012",
        normalizedReference: String? = "123456789012",
    ) = Transaction(
        id = id,
        amount = Money.fromMajorUnits(BigDecimal("1200.00")),
        type = TransactionType.DEBIT,
        transactionDate = transactionDate,
        merchantName = merchantName,
        referenceNumber = referenceNumber,
        normalizedReference = normalizedReference,
        accountIdentifier = accountIdentifier,
        bankName = bankName,
        paymentMethod = paymentMethod,
        categoryId = categoryId,
        primarySource = source,
        fingerprint = "v1:test-fingerprint",
        parseStatus = parseStatus,
        createdAt = Instant.parse("2026-09-17T09:44:00Z"),
        updatedAt = Instant.parse("2026-09-17T09:44:00Z"),
    )
}
