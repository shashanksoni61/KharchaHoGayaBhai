package com.shashanksoni.kharchahogayabhai.sms

import com.shashanksoni.kharchahogayabhai.domain.model.ParseStatus
import com.shashanksoni.kharchahogayabhai.domain.model.PaymentMethod
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class SmsTransactionParserTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val parser = SmsTransactionParser(zone = zone)

    @Test
    fun parsesHdfcUpiDebitWithRef() {
        val body = "HDFC Bank: Rs.250.00 debited from a/c XX1234 on 15-09-26 to VPA swiggy@paytm " +
            "UPI Ref 376598505975. Not you? Call 18002586161"
        val parsed = parser.parse(single(body)).single()

        assertEquals(25_000L, parsed.amount.minorUnits)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals("376598505975", parsed.referenceNumber)
        assertEquals(PaymentMethod.UPI, parsed.paymentMethod)
        assertEquals(TransactionSource.SMS, parsed.source)
        assertEquals("sms:101", parsed.sourceIdentifier)
        assertEquals(ParseStatus.PARSED, parsed.parseStatus)
        assertNotNull(parsed.merchantName)
    }

    @Test
    fun parsesCreditAlert() {
        val body = "Your a/c XX9876 is credited with INR 5,000.00 on 15-09-2026 by UPI Ref No. 451236987410"
        val parsed = parser.parse(single(body)).single()

        assertEquals(500_000L, parsed.amount.minorUnits)
        assertEquals(TransactionType.CREDIT, parsed.type)
        assertEquals("451236987410", parsed.referenceNumber)
    }

    @Test
    fun parsesAxisMultilineDebitWithBodyDateTime() {
        val body = """
            INR 30.00 debited
            A/c no. XX2073
            22-09-26, 20:41:05
            UPI/P2M/347484353597/SHREE MEDICAL STORE
            Not you? SMS BLOCKUPI Cust ID to 919951860002
            Axis Bank
        """.trimIndent()
        val parsed = parser.parse(single(body)).single()

        assertEquals(3_000L, parsed.amount.minorUnits)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals("347484353597", parsed.referenceNumber)
        assertEquals("XX2073", parsed.accountIdentifier)
        assertEquals("SHREE MEDICAL STORE", parsed.merchantName)
        assertEquals(PaymentMethod.UPI, parsed.paymentMethod)
        assertEquals(ParseStatus.PARSED, parsed.parseStatus)
        val expected = LocalDateTime.of(2026, 9, 22, 20, 41, 5)
            .atZone(zone)
            .toInstant()
        assertEquals(expected, parsed.transactionDate)
    }

    @Test
    fun parsesMultipleMessagesFromSameSenderIndependently() {
        val input = SmsParseInput(
            listOf(
                message(
                    body = """
                        INR 30.00 debited
                        A/c no. XX2073
                        22-09-26, 10:00:00
                        UPI/P2M/111111111111/STORE ONE
                        Axis Bank
                    """.trimIndent(),
                    id = 10L,
                    address = "AX-AXISBK",
                ),
                message(
                    body = """
                        INR 150.50 debited
                        A/c no. XX2073
                        22-09-26, 11:30:00
                        UPI/P2M/222222222222/STORE TWO
                        Axis Bank
                    """.trimIndent(),
                    id = 11L,
                    address = "AX-AXISBK",
                ),
                message(
                    body = """
                        INR 1,000.00 credited
                        A/c no. XX2073
                        21-09-26, 09:15:00
                        UPI/P2A/333333333333/SALARY CREDIT
                        Axis Bank
                    """.trimIndent(),
                    id = 12L,
                    address = "AX-AXISBK",
                ),
            ),
        )
        val parsed = parser.parse(input)
        assertEquals(3, parsed.size)
        assertEquals(listOf("111111111111", "222222222222", "333333333333"), parsed.map { it.referenceNumber })
        assertEquals(TransactionType.CREDIT, parsed[2].type)
    }

    @Test
    fun skipsNonTransactionSms() {
        val body = "Your OTP for login is 482910. Do not share with anyone."
        val input = SmsParseInput(listOf(message(body)))
        assertTrue(!parser.canParse(input))
        assertTrue(parser.parse(input).isEmpty())
    }

    @Test
    fun keepsSameSmsIdAsSourceKeyForDedup() {
        val body = "SBI: Debited INR 120.00 on 15Sep26 to ZOMATO. UPI:432109876543. A/c X1234"
        val first = parser.parse(single(body, id = 55)).single()
        val second = parser.parse(single(body, id = 55)).single()
        assertEquals(first.sourceIdentifier, second.sourceIdentifier)
        assertEquals("432109876543", first.referenceNumber)
    }

    private fun single(body: String, id: Long = 101L) =
        SmsParseInput(listOf(message(body, id)))

    private fun message(
        body: String,
        id: Long = 101L,
        address: String = "HDFCBK",
    ) = SmsMessage(
        id = id,
        address = address,
        body = body,
        receivedAt = Instant.parse("2026-09-15T10:30:00Z"),
    )
}
