package com.shashanksoni.kharchahogayabhai.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import java.time.Instant

/**
 * Reads past inbox SMS from the device. Requires [android.Manifest.permission.READ_SMS].
 * Nothing is uploaded — messages stay on-device and are only passed to the local parser.
 */
class SmsInboxReader(
    private val appContext: Context,
) {

    /**
     * @param afterExclusiveMillis when non-null, only messages with DATE strictly
     * after this epoch millis are returned (incremental scan).
     */
    fun readInbox(
        afterExclusiveMillis: Long? = null,
        limit: Int = DEFAULT_LIMIT,
    ): List<SmsMessage> {
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
        )
        val selection: String?
        val selectionArgs: Array<String>?
        if (afterExclusiveMillis != null && afterExclusiveMillis > 0L) {
            selection = "${Telephony.Sms.DATE} > ?"
            selectionArgs = arrayOf(afterExclusiveMillis.toString())
        } else {
            selection = null
            selectionArgs = null
        }

        val results = mutableListOf<SmsMessage>()
        appContext.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (cursor.moveToNext() && results.size < limit) {
                val body = cursor.getString(bodyIndex)?.trim().orEmpty()
                if (body.isEmpty()) continue
                results += SmsMessage(
                    id = cursor.getLong(idIndex),
                    address = cursor.getString(addressIndex),
                    body = body,
                    receivedAt = Instant.ofEpochMilli(cursor.getLong(dateIndex)),
                )
            }
        }
        return results
    }

    companion object {
        const val DEFAULT_LIMIT = 5_000
    }
}
