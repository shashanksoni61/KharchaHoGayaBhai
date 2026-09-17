package com.shashanksoni.kharchahogayabhai.core.normalization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MerchantNameNormalizerTest {

    @Test
    fun `the same merchant from different sources yields one key`() {
        val fromSms = MerchantNameNormalizer.canonicalKey("Swiggy")
        val fromCsv = MerchantNameNormalizer.canonicalKey("SWIGGY")
        val fromPdf = MerchantNameNormalizer.canonicalKey("UPI/SWIGGY/123456789012")
        val fromVpa = MerchantNameNormalizer.canonicalKey("swiggy@icici")

        assertEquals("SWIGGY", fromSms)
        assertEquals(fromSms, fromCsv)
        assertEquals(fromSms, fromPdf)
        assertEquals(fromSms, fromVpa)
    }

    @Test
    fun `company suffixes and payment plumbing are dropped`() {
        assertEquals("SWIGGY", MerchantNameNormalizer.canonicalKey("Swiggy Private Limited"))
        assertEquals("AMAZON", MerchantNameNormalizer.canonicalKey("UPI-AMAZON-PAYMENT"))
        assertEquals("BIGBASKET", MerchantNameNormalizer.canonicalKey("POS BIG BASKET"))
    }

    @Test
    fun `spacing differences do not change the key`() {
        assertEquals(
            MerchantNameNormalizer.canonicalKey("SWIGGY INSTAMART"),
            MerchantNameNormalizer.canonicalKey("Swiggy-Instamart"),
        )
    }

    @Test
    fun `different merchants keep different keys`() {
        val swiggy = MerchantNameNormalizer.canonicalKey("Swiggy")
        val zomato = MerchantNameNormalizer.canonicalKey("Zomato")
        assert(swiggy != zomato)
    }

    @Test
    fun `reference numbers are not mistaken for a merchant`() {
        assertNull(MerchantNameNormalizer.canonicalKey("123456789012"))
        assertNull(MerchantNameNormalizer.canonicalKey("9876543210@ybl"))
        assertEquals("SWIGGY", MerchantNameNormalizer.canonicalKey("SWIGGY 123456789012"))
    }

    @Test
    fun `nothing identifying yields null`() {
        assertNull(MerchantNameNormalizer.canonicalKey(null))
        assertNull(MerchantNameNormalizer.canonicalKey("   "))
        assertNull(MerchantNameNormalizer.canonicalKey("UPI PAYMENT TO"))
    }

    @Test
    fun `display names are readable`() {
        assertEquals("Swiggy", MerchantNameNormalizer.toDisplayName("SWIGGY LIMITED"))
        assertEquals("Swiggy", MerchantNameNormalizer.toDisplayName("UPI/SWIGGY/123456789012"))
        assertEquals("BIG Bazaar", MerchantNameNormalizer.toDisplayName("BIG BAZAAR"))
        assertEquals("KFC", MerchantNameNormalizer.toDisplayName("KFC"))
    }

    @Test
    fun `display name falls back to the original when nothing survives cleaning`() {
        assertEquals("123456789012", MerchantNameNormalizer.toDisplayName("123456789012"))
    }
}
