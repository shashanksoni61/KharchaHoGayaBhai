package com.shashanksoni.kharchahogayabhai.core.deduplication

import com.shashanksoni.kharchahogayabhai.core.normalization.TransactionNormalizer
import com.shashanksoni.kharchahogayabhai.domain.model.Money
import com.shashanksoni.kharchahogayabhai.domain.model.ParsedTransaction
import com.shashanksoni.kharchahogayabhai.domain.model.PaymentMethod
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The behaviour the whole app depends on: one real payment must produce one
 * identity, no matter which source described it, and two different payments must
 * never collapse into one.
 */
class CrossSourceDeduplicationTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private val normalizer = TransactionNormalizer(
        zone = zone,
        clock = Clock.fixed(Instant.parse("2026-09-18T04:30:00Z"), zone),
    )

    private val paymentDate: LocalDate = LocalDate.of(2026, 9, 17)

    @Test
    fun `SMS and CSV reporting the same payment share an identity`() {
        assertEquals(fingerprintOf(swiggySms()), fingerprintOf(swiggyCsv()))
    }

    @Test
    fun `SMS and PDF reporting the same payment share an identity`() {
        assertEquals(fingerprintOf(swiggySms()), fingerprintOf(swiggyPdf()))
    }

    @Test
    fun `CSV and PDF reporting the same payment share an identity`() {
        assertEquals(fingerprintOf(swiggyCsv()), fingerprintOf(swiggyPdf()))
    }

    @Test
    fun `all three sources collapse to a single transaction`() {
        val fingerprints = listOf(swiggySms(), swiggyCsv(), swiggyPdf())
            .map(::fingerprintOf)
            .distinct()

        assertEquals(1, fingerprints.size)
    }

    @Test
    fun `a reference written differently is still the same reference`() {
        val spaced = fingerprintOf(swiggySms(reference = "UPI Ref: 1234 5678 9012"))
        val bare = fingerprintOf(swiggyCsv(reference = "123456789012"))
        val labelled = fingerprintOf(swiggyPdf(reference = "Txn ID 123456789012"))

        assertEquals(spaced, bare)
        assertEquals(spaced, labelled)
    }

    @Test
    fun `a trusted reference outweighs merchant spelling and missing time`() {
        val sms = fingerprintOf(swiggySms(merchant = "Swiggy", time = LocalTime.of(15, 14)))
        val statement = fingerprintOf(
            swiggyCsv(merchant = "SWIGGY LIMITED", time = LocalTime.MIDNIGHT),
        )

        assertEquals(sms, statement)
    }

    @Test
    fun `same amount but different references are different payments`() {
        assertNotEquals(
            fingerprintOf(swiggySms(reference = "UPI Ref: 123456789012")),
            fingerprintOf(swiggySms(reference = "UPI Ref: 999999999999")),
        )
    }

    @Test
    fun `a refund is not the same as the payment it reverses`() {
        assertNotEquals(
            fingerprintOf(swiggySms(type = TransactionType.DEBIT)),
            fingerprintOf(swiggySms(type = TransactionType.CREDIT)),
        )
    }

    @Test
    fun `payments without a reference are matched on their fields`() {
        val sms = fingerprintOf(
            swiggySms(reference = null, merchant = "Apollo Pharmacy", account = "XX4321"),
        )
        val csv = fingerprintOf(
            swiggyCsv(
                reference = null,
                merchant = "APOLLO PHARMACY",
                account = "XXXXXXXX4321",
                time = LocalTime.MIDNIGHT,
            ),
        )

        assertEquals(sms, csv)
    }

    @Test
    fun `without a reference, the same merchant on different days stays separate`() {
        val onTheSeventeenth = fingerprintOf(swiggySms(reference = null))
        val onTheEighteenth = fingerprintOf(
            swiggySms(reference = null, date = paymentDate.plusDays(1)),
        )

        assertNotEquals(onTheSeventeenth, onTheEighteenth)
    }

    @Test
    fun `without a reference, different merchants on the same day stay separate`() {
        assertNotEquals(
            fingerprintOf(swiggySms(reference = null, merchant = "Swiggy")),
            fingerprintOf(swiggySms(reference = null, merchant = "Zomato")),
        )
    }

    @Test
    fun `without a reference, different amounts stay separate`() {
        assertNotEquals(
            fingerprintOf(swiggySms(reference = null, amountMajor = "540.00")),
            fingerprintOf(swiggySms(reference = null, amountMajor = "540.50")),
        )
    }

    @Test
    fun `a reference too weak to trust falls back to field matching`() {
        val weakReference = fingerprintOf(swiggySms(reference = "Ref 12"))
        val noReference = fingerprintOf(swiggySms(reference = null))

        assertEquals(noReference, weakReference)
    }

    @Test
    fun `normalisation keeps the reference and account in canonical form`() {
        val transaction = normalizer.normalize(swiggySms()).transaction

        assertEquals("123456789012", transaction.normalizedReference)
        assertEquals("1234", transaction.accountIdentifier)
        assertEquals("Swiggy", transaction.merchantName)
        assertEquals(54000L, transaction.amount.minorUnits)
    }

    private fun fingerprintOf(parsed: ParsedTransaction): String =
        normalizer.normalize(parsed).transaction.fingerprint

    private fun swiggySms(
        amountMajor: String = "540.00",
        type: TransactionType = TransactionType.DEBIT,
        date: LocalDate = paymentDate,
        time: LocalTime = LocalTime.of(15, 14),
        merchant: String? = "Swiggy",
        reference: String? = "UPI Ref: 123456789012",
        account: String? = "XX1234",
    ) = ParsedTransaction(
        amount = Money.fromMajorUnits(BigDecimal(amountMajor)),
        type = type,
        transactionDate = date.atTime(time).atZone(zone).toInstant(),
        merchantName = merchant,
        referenceNumber = reference,
        accountIdentifier = account,
        bankName = "HDFC Bank",
        paymentMethod = PaymentMethod.UPI,
        source = TransactionSource.SMS,
        sourceIdentifier = "sms-1",
    )

    private fun swiggyCsv(
        amountMajor: String = "540.00",
        date: LocalDate = paymentDate,
        time: LocalTime = LocalTime.MIDNIGHT,
        merchant: String? = "SWIGGY",
        reference: String? = "123456789012",
        account: String? = "XXXXXXXX1234",
    ) = ParsedTransaction(
        amount = Money.fromMajorUnits(BigDecimal(amountMajor)),
        type = TransactionType.DEBIT,
        transactionDate = date.atTime(time).atZone(zone).toInstant(),
        description = "UPI-SWIGGY-123456789012",
        merchantName = merchant,
        referenceNumber = reference,
        accountIdentifier = account,
        bankName = "HDFC Bank",
        source = TransactionSource.CSV,
        sourceIdentifier = "csv-row-42",
    )

    private fun swiggyPdf(
        amountMajor: String = "540.00",
        date: LocalDate = paymentDate,
        merchant: String? = "UPI/SWIGGY/123456789012",
        reference: String? = "UPI Ref No 123456789012",
    ) = ParsedTransaction(
        amount = Money.fromMajorUnits(BigDecimal(amountMajor)),
        type = TransactionType.DEBIT,
        transactionDate = date.atStartOfDay(zone).toInstant(),
        description = "UPI/SWIGGY/123456789012/PAYMENT",
        merchantName = merchant,
        referenceNumber = reference,
        accountIdentifier = "1234",
        bankName = "HDFC Bank",
        source = TransactionSource.PDF,
        sourceIdentifier = "pdf-page3-line12",
    )
}
