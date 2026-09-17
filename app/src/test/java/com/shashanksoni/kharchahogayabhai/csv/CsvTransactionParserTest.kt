package com.shashanksoni.kharchahogayabhai.csv

import com.shashanksoni.kharchahogayabhai.core.normalization.TransactionNormalizer
import com.shashanksoni.kharchahogayabhai.domain.model.ParseStatus
import com.shashanksoni.kharchahogayabhai.domain.model.PaymentMethod
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CsvTransactionParserTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val parser = CsvTransactionParser(zone = zone)

    @Test
    fun `parses the real PhonePe statement by content not headers`() {
        val csv = readResource("PhonePe_Statement_Aug2026_Sept2026.csv")
        val input = CsvParseInput(csv, "PhonePe_Statement_Aug2026_Sept2026.csv")

        assertTrue(parser.canParse(input))
        val parsed = parser.parse(input)

        assertTrue("expected many rows, got ${parsed.size}", parsed.size >= 150)
        assertTrue(parsed.none { it.parseStatus == ParseStatus.FAILED })

        val first = parsed.first()
        assertEquals(TransactionType.DEBIT, first.type)
        assertEquals(1000L, first.amount.minorUnits)
        assertEquals("376598505975", first.referenceNumber)
        assertEquals("LAXMAN DAIRY AND PROVISION STORE", first.merchantName)
        assertEquals(PaymentMethod.UPI, first.paymentMethod)
        assertEquals("PhonePe", first.bankName)
        assertTrue(first.accountIdentifier!!.endsWith("682073") || first.accountIdentifier == "682073")
        assertTrue(first.sourceIdentifier!!.startsWith("phonepe:T260917"))

        val expectedInstant = LocalDate.of(2026, 9, 17)
            .atTime(LocalTime.of(21, 51))
            .atZone(zone)
            .toInstant()
        assertEquals(expectedInstant, first.transactionDate)

        val credit = parsed.first { it.type == TransactionType.CREDIT }
        assertTrue(credit.amount.minorUnits > 0)

        val normalizer = TransactionNormalizer(zone = zone)
        val fingerprints = parsed
            .filter { it.referenceNumber == "376598505975" }
            .map { normalizer.normalize(it).transaction.fingerprint }
            .distinct()
        assertEquals(1, fingerprints.size)
    }

    @Test
    fun `still works when PhonePe columns are renamed and reordered`() {
        val csv = """
            When,Clock,What happened,BankRef,UpiRef,Direction,Wallet,Rupees
            "Sept 17, 2026","09:51 pm","Paid to LAXMAN DAIRY","T2609172151170190431448","376598505975","DEBIT","Paid by XXXX682073","10"
            "Sept 09, 2026","05:38 am","Received from Yaseen Khan","T2609090538406807314280","923647242229","CREDIT","Credited to XXXX682073","50"
        """.trimIndent()

        val parsed = parser.parse(CsvParseInput(csv, "renamed.csv"))
        assertEquals(2, parsed.size)
        assertEquals("376598505975", parsed[0].referenceNumber)
        assertEquals(1000L, parsed[0].amount.minorUnits)
        assertEquals(TransactionType.CREDIT, parsed[1].type)
        assertEquals(5000L, parsed[1].amount.minorUnits)
    }

    @Test
    fun `works with no header row at all`() {
        val csv = """
            17/09/2026,Swiggy UPI,540.00,DR,123456789012
            01/09/2026,Salary NEFT,95000.00,CR,AXISN12398745612
        """.trimIndent()

        val parsed = parser.parse(CsvParseInput(csv, "bare.csv"))
        assertEquals(2, parsed.size)
        assertEquals(54000L, parsed[0].amount.minorUnits)
        assertEquals(TransactionType.DEBIT, parsed[0].type)
        assertEquals("123456789012", parsed[0].referenceNumber)
        assertEquals(TransactionType.CREDIT, parsed[1].type)
    }

    @Test
    fun `debit credit balance layout still resolves the transaction amount`() {
        val csv = """
            Date,Narration,Withdrawal,Deposit,Closing Balance,UTR
            16-09-2026,UPI-AMAZON-451236987410,1299.00,,5000.00,451236987410
        """.trimIndent()

        val parsed = parser.parse(CsvParseInput(csv, "icici.csv"))
        assertEquals(1, parsed.size)
        assertEquals(129900L, parsed[0].amount.minorUnits)
        assertEquals("451236987410", parsed[0].referenceNumber)
    }

    @Test
    fun `boilerplate PhonePe footer is ignored`() {
        val csv = readResource("PhonePe_Statement_Aug2026_Sept2026.csv")
        val parsed = parser.parse(CsvParseInput(csv, "PhonePe_Statement_Aug2026_Sept2026.csv"))
        assertTrue(parsed.none { it.description?.contains("automatically generated") == true })
        assertTrue(parsed.none { it.description?.contains("Disclaimer") == true })
    }

    private fun readResource(name: String): String =
        checkNotNull(javaClass.classLoader.getResourceAsStream(name)) {
            "Missing test resource $name"
        }.bufferedReader().use { it.readText() }
}
