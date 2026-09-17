package com.shashanksoni.kharchahogayabhai.core.normalization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionReferenceNormalizerTest {

    @Test
    fun `the same reference labelled differently normalises identically`() {
        val expected = "123456789012"
        assertEquals(expected, TransactionReferenceNormalizer.normalize("UPI Ref: 123456789012"))
        assertEquals(expected, TransactionReferenceNormalizer.normalize("Transaction ID 123456789012"))
        assertEquals(expected, TransactionReferenceNormalizer.normalize("UPI Ref No 123456789012"))
        assertEquals(expected, TransactionReferenceNormalizer.normalize("123456789012"))
        assertEquals(expected, TransactionReferenceNormalizer.normalize("Txn ID: 123456789012."))
    }

    @Test
    fun `formatting inside the reference is stripped`() {
        assertEquals("123456789012", TransactionReferenceNormalizer.normalize("1234 5678 9012"))
        assertEquals("123456789012", TransactionReferenceNormalizer.normalize("1234-5678-9012"))
    }

    @Test
    fun `a reference embedded in a narration is recovered`() {
        assertEquals(
            "123456789012",
            TransactionReferenceNormalizer.normalize("UPI/123456789012/SWIGGY"),
        )
    }

    @Test
    fun `alphanumeric bank references are kept`() {
        assertEquals("AXISN12398745612", TransactionReferenceNormalizer.normalize("AXISN12398745612"))
        assertEquals("IMPS784512369874", TransactionReferenceNormalizer.normalize("UTR: IMPS784512369874"))
    }

    @Test
    fun `references too weak to identify a transaction are rejected`() {
        assertNull(TransactionReferenceNormalizer.normalize("Ref 12"))
        assertNull(TransactionReferenceNormalizer.normalize("UPI"))
        assertNull(TransactionReferenceNormalizer.normalize("N/A"))
        assertNull(TransactionReferenceNormalizer.normalize(null))
        assertNull(TransactionReferenceNormalizer.normalize(""))
    }

    @Test
    fun `different references stay different`() {
        assertEquals(null, TransactionReferenceNormalizer.normalize("-"))
        assert(
            TransactionReferenceNormalizer.normalize("123456789012") !=
                TransactionReferenceNormalizer.normalize("123456789013"),
        )
    }
}
