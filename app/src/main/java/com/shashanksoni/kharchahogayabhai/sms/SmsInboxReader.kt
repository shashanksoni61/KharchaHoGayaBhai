package com.shashanksoni.kharchahogayabhai.sms

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.Telephony
import java.time.Instant

/** One page of inbox rows, including how far the cursor actually moved. */
data class SmsPage(
    val messages: List<SmsMessage>,
    val rowsRead: Int,
)

/**
 * Reads past SMS from the device. Requires [android.Manifest.permission.READ_SMS].
 * Nothing is uploaded — messages stay on-device and are only passed to the local parser.
 *
 * Real phones often cap an unpaged inbox query at a few thousand rows. Pages use
 * `limit`/`offset` so a 20k+ mailbox is walked to the end.
 */
class SmsInboxReader(
    private val appContext: Context,
) {

    fun countInbox(): Int = count(Telephony.Sms.Inbox.CONTENT_URI)

    /** Inbox + sent + drafts, so the user can compare with apps that count every SMS. */
    fun countAllSms(): Int = count(Telephony.Sms.CONTENT_URI)

    /**
     * Inbox page at [offset], oldest first. [rowsRead] is how many provider rows
     * were consumed (including empty bodies) so the next offset does not skip or
     * repeat messages.
     */
    fun readInboxPage(offset: Int, limit: Int = DEFAULT_PAGE_SIZE): SmsPage {
        require(offset >= 0) { "offset must be >= 0" }
        require(limit > 0) { "limit must be > 0" }
        readWithUriPaging(offset, limit)?.let { if (it.rowsRead > 0 || offset == 0) return it }
        return readWithBundlePaging(offset, limit)
    }

    /**
     * Messages strictly after [afterExclusive], one page. Used for incremental
     * scans of new SMS only.
     */
    fun readInboxAfter(afterExclusive: SmsScanCursor, limit: Int = DEFAULT_PAGE_SIZE): SmsPage {
        val selection =
            "(${Telephony.Sms.DATE} > ?) OR " +
                "(${Telephony.Sms.DATE} = ? AND ${Telephony.Sms._ID} > ?)"
        val selectionArgs = arrayOf(
            afterExclusive.dateMillis.toString(),
            afterExclusive.dateMillis.toString(),
            afterExclusive.smsId.toString(),
        )
        val uri = Telephony.Sms.Inbox.CONTENT_URI.buildUpon()
            .appendQueryParameter("limit", limit.toString())
            .build()
        return queryToPage(
            uri = uri,
            selection = selection,
            selectionArgs = selectionArgs,
            extras = null,
        )
    }

    private fun count(uri: android.net.Uri): Int {
        appContext.contentResolver.query(
            uri,
            arrayOf(Telephony.Sms._ID),
            null,
            null,
            null,
        )?.use { cursor ->
            return cursor.count
        }
        return 0
    }

    private fun readWithUriPaging(offset: Int, limit: Int): SmsPage? {
        // content://sms + type=inbox + _id order returns every row from the
        // same sender. Inbox URI + date order on some phones skips thread siblings.
        val uri = Telephony.Sms.CONTENT_URI.buildUpon()
            .appendQueryParameter("limit", limit.toString())
            .appendQueryParameter("offset", offset.toString())
            .build()
        return runCatching {
            queryToPage(
                uri = uri,
                selection = "${Telephony.Sms.TYPE} = ?",
                selectionArgs = arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString()),
                extras = null,
                sortOrder = "${Telephony.Sms._ID} ASC",
            )
        }.getOrNull()
    }

    private fun readWithBundlePaging(offset: Int, limit: Int): SmsPage {
        val extras = Bundle().apply {
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(Telephony.Sms._ID))
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_ASCENDING,
            )
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${Telephony.Sms.TYPE} = ?")
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString()),
            )
        }
        return queryToPage(
            uri = Telephony.Sms.CONTENT_URI,
            selection = null,
            selectionArgs = null,
            extras = extras,
        )
    }

    private fun queryToPage(
        uri: android.net.Uri,
        selection: String?,
        selectionArgs: Array<String>?,
        extras: Bundle?,
        sortOrder: String = "${Telephony.Sms.DATE} ASC, ${Telephony.Sms._ID} ASC",
    ): SmsPage {
        val messages = mutableListOf<SmsMessage>()
        var rowsRead = 0
        val cursor = if (extras != null) {
            appContext.contentResolver.query(
                uri,
                PROJECTION,
                extras,
                null as CancellationSignal?,
            )
        } else {
            appContext.contentResolver.query(
                uri,
                PROJECTION,
                selection,
                selectionArgs,
                sortOrder,
            )
        }
        cursor?.use { rows ->
            val idIndex = rows.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = rows.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = rows.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = rows.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (rows.moveToNext()) {
                rowsRead += 1
                val body = rows.getString(bodyIndex)?.trim().orEmpty()
                if (body.isEmpty()) continue
                messages += SmsMessage(
                    id = rows.getLong(idIndex),
                    address = rows.getString(addressIndex),
                    body = body,
                    receivedAt = Instant.ofEpochMilli(rows.getLong(dateIndex)),
                )
            }
        }
        return SmsPage(messages = messages, rowsRead = rowsRead)
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 500
        private val PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
        )
    }
}
