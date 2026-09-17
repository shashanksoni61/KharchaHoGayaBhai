package com.shashanksoni.kharchahogayabhai.core.normalization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountIdentifierNormalizerTest {

    @Test
    fun `however a source masks the account, the last four digits win`() {
        assertEquals("1234", AccountIdentifierNormalizer.normalize("XX1234"))
        assertEquals("1234", AccountIdentifierNormalizer.normalize("****1234"))
        assertEquals("1234", AccountIdentifierNormalizer.normalize("A/c XXXXXX1234"))
        assertEquals("1234", AccountIdentifierNormalizer.normalize("50100123451234"))
    }

    @Test
    fun `too few digits to identify an account yields null`() {
        assertNull(AccountIdentifierNormalizer.normalize("XX1"))
        assertNull(AccountIdentifierNormalizer.normalize("savings"))
        assertNull(AccountIdentifierNormalizer.normalize(null))
    }

    @Test
    fun `display form is masked`() {
        assertEquals("\u2022\u2022\u2022\u20221234", AccountIdentifierNormalizer.toMaskedDisplay("1234"))
        assertNull(AccountIdentifierNormalizer.toMaskedDisplay(null))
    }
}
