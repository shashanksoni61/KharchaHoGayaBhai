package com.shashanksoni.kharchahogayabhai.sms

/** Live counters while an inbox scan is walking through pages of SMS. */
data class SmsScanProgress(
    val scannedCount: Int,
    val inboxTotal: Int,
    val allSmsCount: Int = 0,
    val parsedCount: Int,
)
