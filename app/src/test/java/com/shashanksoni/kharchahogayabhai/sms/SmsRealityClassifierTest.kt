package com.shashanksoni.kharchahogayabhai.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsRealityClassifierTest {

    @Test
    fun featureCountMatchesWeights() {
        val features = SmsRealityFeatures.extract(
            "INR 30.00 debited A/c no. XX2073 UPI/P2M/347484353597/STORE",
            "AX-AXISBK",
        )
        assertEquals(SmsRealityModelWeights.WEIGHTS.size, features.size)
        assertEquals(SmsRealityModelWeights.FEATURE_NAMES.size, features.size)
    }

    @Test
    fun scoresLimitAlertAsNotAPayment() {
        assertTrue(
            SmsRealityClassifier.isNotAPayment(
                "Your remaining credit limit for the card is 1,38,717 INR",
                "VM-HDFCBK",
            ),
        )
    }

    @Test
    fun scoresOfferAsNotAPayment() {
        assertTrue(
            SmsRealityClassifier.isNotAPayment(
                "Win Rs 1000 cashback. Promotional offer. Terms apply.",
                "LM-PHONEP",
            ),
        )
    }

    @Test
    fun scoresUpiDebitAsAPayment() {
        assertTrue(
            !SmsRealityClassifier.isNotAPayment(
                "INR 30.00 debited A/c no. XX2073 22-09-26 UPI/P2M/347484353597/STORE Not you? SMS BLOCKUPI Axis Bank",
                "AX-AXISBK",
            ),
        )
    }

    @Test
    fun scoresCardSpendAsAPaymentEvenWhenLimitIsPrinted() {
        assertTrue(
            !SmsRealityClassifier.isNotAPayment(
                "Thank you for using your HDFC Bank Credit Card ending 1234 for Rs.25.00 at AMAZON. Available limit: Rs 1,38,717.00",
                "HDFCBK",
            ),
        )
    }
}
