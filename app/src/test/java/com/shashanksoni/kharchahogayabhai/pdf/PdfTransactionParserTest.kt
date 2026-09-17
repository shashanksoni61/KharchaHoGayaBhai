package com.shashanksoni.kharchahogayabhai.pdf

import com.shashanksoni.kharchahogayabhai.domain.model.ParseStatus
import com.shashanksoni.kharchahogayabhai.domain.model.PaymentMethod
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class PdfTransactionParserTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val parser = PdfTransactionParser(zone = zone)

    @Test
    fun `parses dated amount lines without relying on layout labels`() {
        val text = """
            Account Statement September 2026
            17/09/2026 UPI/SWIGGY/123456789012/PAYMENT 540.00 12,000.00
            16/09/2026 UPI/AMAZON/451236987410 1,299.00 12,540.00
            01/09/2026 NEFT SALARY CREDIT AXISN12398745612 95,000.00 1,07,839.00
        """.trimIndent()

        assertTrue(parser.canParse(PdfParseInput(text, "statement.pdf")))
        val parsed = parser.parse(PdfParseInput(text, "statement.pdf"))
        assertTrue(parsed.size >= 2)

        val swiggy = parsed.first { it.referenceNumber?.contains("123456789012") == true }
        assertEquals(54000L, swiggy.amount.minorUnits)
        assertEquals(TransactionType.DEBIT, swiggy.type)
    }

    @Test
    fun `joins multi-line narration onto the dated row`() {
        val text = """
            17/09/2026 Paid to Swiggy Instamart
            UPI Ref 998877665544
            540.00 DR
        """.trimIndent()

        val parsed = parser.parse(PdfParseInput(text, "multi.pdf"))
        assertEquals(1, parsed.size)
        assertEquals(54000L, parsed[0].amount.minorUnits)
        assertTrue(
            parsed[0].merchantName?.contains("Swiggy", ignoreCase = true) == true ||
                parsed[0].description?.contains("Swiggy", ignoreCase = true) == true,
        )
    }

    @Test
    fun `parses Google Pay layout statement with date paid-to amount and UPI id`() {
        val text = """
            Transaction statement
            Transaction statement period                                                  Sent                                     Received
            01 August 2026 - 31 August 2026                                          ₹40,578.90                                        ₹0

            Date & time                            Transaction details                                                                         Amount

            02 Aug, 2026                           Paid to Deepak Kumar Chouksey                                                                 ₹1,000
            12:59 PM                               UPI Transaction ID: 658009337411
                                                       Paid by Axis Bank 2073

            14 Aug, 2026                           Paid to Jio Prepaid                                                                         ₹900.90
            10:50 AM                               UPI Transaction ID: 659272781505
                                                       Paid by Axis Bank 2073

            Note: This statement reflects payments made by you on the Google Pay app.
        """.trimIndent()

        assertTrue(parser.canParse(PdfParseInput(text, "gpay.pdf")))
        val parsed = parser.parse(PdfParseInput(text, "gpay.pdf"))
            .filter { it.parseStatus != ParseStatus.FAILED }

        assertEquals(2, parsed.size)

        val deepak = parsed.first { it.referenceNumber == "658009337411" }
        assertEquals(100_000L, deepak.amount.minorUnits)
        assertEquals(TransactionType.DEBIT, deepak.type)
        assertEquals(PaymentMethod.UPI, deepak.paymentMethod)
        assertTrue(deepak.merchantName?.contains("Deepak", ignoreCase = true) == true)
        val deepakLocal = deepak.transactionDate.atZone(zone)
        assertEquals(LocalDate.of(2026, 8, 2), deepakLocal.toLocalDate())
        assertEquals(LocalTime.of(12, 59), deepakLocal.toLocalTime())

        val jio = parsed.first { it.referenceNumber == "659272781505" }
        assertEquals(90_090L, jio.amount.minorUnits)
        assertTrue(jio.merchantName?.contains("Jio", ignoreCase = true) == true)

        // Period totals must not become a transaction.
        assertTrue(parsed.none { it.amount.minorUnits == 4_057_890L })
    }

    @Test
    fun `parses Google Pay stacked PdfBox-style text`() {
        val text = """
            Transactionstatement
            Note: This statement reflects payments made by you on the Google Pay app.
            Transactionstatementperiod
            01August2026-31August2026
            Sent
            ₹40,578.90
            Received
            ₹0
            Date&time Transactiondetails Amount
            02 Aug, 2026
            12:59PM
            Paid to Deepak Kumar Chouksey
            UPITransactionID:658009337411
            PaidbyAxisBank2073
            ₹1,000
            26 Aug, 2026
            11:22PM
            Paid to Google Play
            UPITransactionID:508371852406
            PaidbyAxisBank2073
            ₹179
        """.trimIndent()

        val parsed = parser.parse(PdfParseInput(text, "gpay_raw.pdf"))
            .filter { it.parseStatus != ParseStatus.FAILED }

        assertEquals(2, parsed.size)
        assertEquals("658009337411", parsed[0].referenceNumber)
        assertEquals(100_000L, parsed[0].amount.minorUnits)
        assertEquals("508371852406", parsed[1].referenceNumber)
        assertEquals(17_900L, parsed[1].amount.minorUnits)
        assertEquals(LocalTime.of(23, 22), parsed[1].transactionDate.atZone(zone).toLocalTime())
    }
}
