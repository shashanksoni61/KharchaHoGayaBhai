package com.shashanksoni.kharchahogayabhai.sms

import kotlin.math.exp

/**
 * Tiny on-device logistic model: is this SMS a real payment, or an offer /
 * limit / due / OTP-style alert?
 *
 * Rules in [SmsTransactionParser] still win on clear bank movement and on
 * already-known promo templates. This only scores the gray zone.
 */
internal object SmsRealityClassifier {
    fun probabilityNotAPayment(body: String, address: String?): Float {
        val features = SmsRealityFeatures.extract(body, address)
        val weights = SmsRealityModelWeights.WEIGHTS
        require(features.size == weights.size) {
            "SMS reality features (${features.size}) != weights (${weights.size})"
        }
        var logit = SmsRealityModelWeights.BIAS
        for (i in weights.indices) {
            logit += weights[i] * features[i]
        }
        return (1.0 / (1.0 + exp(-logit.toDouble()))).toFloat()
    }

    fun isNotAPayment(body: String, address: String?): Boolean =
        probabilityNotAPayment(body, address) >= SmsRealityModelWeights.NOT_A_PAYMENT_THRESHOLD
}
