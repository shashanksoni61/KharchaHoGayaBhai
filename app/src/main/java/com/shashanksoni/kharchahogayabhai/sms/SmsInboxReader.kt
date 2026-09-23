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
     * @param afterExclusive when non-null, only messages strictly after this
     * (date, id) cursor are returned so same-timestamp bursts from one sender
     * are not skipped.
     */
    /** How many inbox rows the provider reports, used for scan progress. */
    fun countInbox(): Int {
        appContext.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            null,
            null,
            null,
        )?.use { cursor ->
            return cursor.count
        }
        return 0
    }

    /**
     * One page of inbox SMS, oldest first. Callers page with [afterExclusive]
     * until a page comes back empty — there is no whole-inbox cap.
     */
    fun readInbox(
        afterExclusive: SmsScanCursor? = null,
        limit: Int = DEFAULT_PAGE_SIZE,
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
        if (afterExclusive != null && afterExclusive.dateMillis > 0L) {
            // (date > last) OR (date = last AND _id > lastId)
            selection =
                "(${Telephony.Sms.DATE} > ?) OR " +
                    "(${Telephony.Sms.DATE} = ? AND ${Telephony.Sms._ID} > ?)"
            selectionArgs = arrayOf(
                afterExclusive.dateMillis.toString(),
                afterExclusive.dateMillis.toString(),
                afterExclusive.smsId.toString(),
            )
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
            "${Telephony.Sms.DATE} ASC, ${Telephony.Sms._ID} ASC",
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
        const val DEFAULT_PAGE_SIZE = 1_000
    }
}
