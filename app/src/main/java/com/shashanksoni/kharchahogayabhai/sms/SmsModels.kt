package com.shashanksoni.kharchahogayabhai.sms

import java.time.Instant

/** One inbox SMS as read from the device content provider. */
data class SmsMessage(
    val id: Long,
    val address: String?,
    val body: String,
    val receivedAt: Instant,
)

/** Input for [SmsTransactionParser]. */
data class SmsParseInput(
    val messages: List<SmsMessage>,
)
