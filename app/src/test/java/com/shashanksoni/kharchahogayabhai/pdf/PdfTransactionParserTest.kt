package com.shashanksoni.kharchahogayabhai.pdf

import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class PdfTransactionParserTest {

    private val parser = PdfTransactionParser(zone = ZoneId.of("Asia/Kolkata"))

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
}
