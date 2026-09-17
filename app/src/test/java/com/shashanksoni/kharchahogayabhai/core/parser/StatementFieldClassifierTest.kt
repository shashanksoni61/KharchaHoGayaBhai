package com.shashanksoni.kharchahogayabhai.core.parser

import com.shashanksoni.kharchahogayabhai.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class StatementFieldClassifierTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun `classifies a PhonePe row without using headers`() {
        val fields = StatementFieldClassifier.classifyAll(
            listOf(
                "Sept 17, 2026",
                "09:51 pm",
                "Paid to LAXMAN DAIRY AND PROVISION STORE",
                "T2609172151170190431448",
                "376598505975",
                "DEBIT",
                "Paid by XXXX682073",
                "10",
            ),
        )

        assertTrue(fields.any { it.kind == StatementFieldKind.DATE })
        assertTrue(fields.any { it.kind == StatementFieldKind.TIME })
        assertTrue(fields.any { it.kind == StatementFieldKind.MONEY && it.money?.minorUnits == 1000L })
        assertTrue(fields.any { it.kind == StatementFieldKind.REFERENCE && it.raw == "376598505975" })
        assertTrue(fields.any { it.kind == StatementFieldKind.TYPE_SIGNAL && it.typeHint == TransactionType.DEBIT })
        assertTrue(fields.any { it.kind == StatementFieldKind.NARRATIVE })

        val row = StatementRowAssembler.assemble(fields, zone)
        assertNotNull(row)
        assertEquals(1000L, row!!.amount.minorUnits)
        assertEquals("376598505975", row.referenceNumber)
        assertEquals("LAXMAN DAIRY AND PROVISION STORE", row.merchantName)
        assertEquals(TransactionType.DEBIT, row.type)
    }

    @Test
    fun `long digit utr is not treated as money`() {
        val field = StatementFieldClassifier.classify("376598505975")
        assertEquals(StatementFieldKind.REFERENCE, field.kind)
    }

    @Test
    fun `picks txn amount not closing balance`() {
        val fields = StatementFieldClassifier.classifyAll(
            listOf("16-09-2026", "AMAZON", "1299.00", "5000.00", "451236987410"),
        )
        val row = StatementRowAssembler.assemble(fields, zone)!!
        assertEquals(129900L, row.amount.minorUnits)
        assertEquals("451236987410", row.referenceNumber)
    }
}
