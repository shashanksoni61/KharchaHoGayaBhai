package com.shashanksoni.kharchahogayabhai.core.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Parses the date/time strings Indian bank and UPI statements typically print.
 * Returns null instead of inventing a date when the value is unrecognised.
 */
object StatementDateParser {

    private val dateFormatters = listOf(
        DateTimeFormatter.ofPattern("d/M/uuuu", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d-M-uuuu", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d/M/uu", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d-M-uu", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("uuuu-M-d", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d MMM, uuuu", Locale.ENGLISH), // GPay: "02 Aug, 2026"
        DateTimeFormatter.ofPattern("d-MMM-uuuu", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d MMMM, uuuu", Locale.ENGLISH),
        // PhonePe: "Sept 17, 2026" (normalised to Sep before parse)
        DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("ddMMyyyy", Locale.ENGLISH),
    )

    private val dateTimeFormatters = listOf(
        DateTimeFormatter.ofPattern("d/M/uuuu H:m:s", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d/M/uuuu H:m", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d-M-uuuu H:m:s", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d-M-uuuu H:m", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("uuuu-M-d H:m:s", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("uuuu-M-d'T'H:m:s", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d MMM uuuu H:m", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM d, uuuu h:mm a", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMMM d, uuuu h:mm a", Locale.ENGLISH),
    )

    private val timeFormatters = listOf(
        DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("h:mm:ss a", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("H:m:s", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("H:m", Locale.ENGLISH),
    )

    fun parseToInstant(raw: String?, zone: ZoneId): java.time.Instant? {
        val cleaned = normalizeMonthNames(raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null)
        parseDateTime(cleaned)?.let { return it.atZone(zone).toInstant() }
        parseDate(cleaned)?.let { return it.atStartOfDay(zone).toInstant() }
        return null
    }

    /**
     * PhonePe and similar exports split date and time into separate columns
     * (`"Sept 17, 2026"` + `"09:51 pm"`).
     */
    fun parseDateAndTime(dateRaw: String?, timeRaw: String?, zone: ZoneId): java.time.Instant? {
        val date = dateRaw?.let { parseDate(normalizeMonthNames(it.trim())) } ?: return null
        val time = timeRaw?.let { parseTime(it.trim()) } ?: LocalTime.MIDNIGHT
        return date.atTime(time).atZone(zone).toInstant()
    }

    fun parseDate(raw: String): LocalDate? {
        val cleaned = normalizeMonthNames(raw.trim())
        for (formatter in dateFormatters) {
            try {
                return LocalDate.parse(cleaned, formatter)
            } catch (_: DateTimeParseException) {
                // try next
            }
        }
        return null
    }

    fun parseTime(raw: String): LocalTime? {
        val spaced = insertSpaceBeforeAmPm(raw.trim())
        val cleaned = spaced.uppercase(Locale.ENGLISH).replace('.', ':')
        for (formatter in timeFormatters) {
            try {
                return LocalTime.parse(cleaned, formatter)
            } catch (_: DateTimeParseException) {
                // try next
            }
        }
        // Retry with original casing for patterns that need "pm"
        for (formatter in timeFormatters) {
            try {
                return LocalTime.parse(spaced, formatter)
            } catch (_: DateTimeParseException) {
                // try next
            }
        }
        return null
    }

    /** GPay / some banks print `12:59PM` without a space before AM/PM. */
    private fun insertSpaceBeforeAmPm(raw: String): String =
        raw.replace(Regex("""(?i)(\d)([ap]m)\b"""), "$1 $2")

    private fun parseDateTime(raw: String): LocalDateTime? {
        val cleaned = normalizeMonthNames(raw.trim())
        for (formatter in dateTimeFormatters) {
            try {
                return LocalDateTime.parse(cleaned, formatter)
            } catch (_: DateTimeParseException) {
                // try next
            }
        }
        val withoutZone = cleaned.replace(Regex("""\s+[A-Za-z]{2,5}$"""), "")
        if (withoutZone != cleaned) {
            for (formatter in dateTimeFormatters) {
                try {
                    return LocalDateTime.parse(withoutZone, formatter)
                } catch (_: DateTimeParseException) {
                    // try next
                }
            }
        }
        return null
    }

    /**
     * PhonePe writes `Sept` / `June` style abbreviations that [DateTimeFormatter]
     * does not always accept under `MMM`.
     */
    private fun normalizeMonthNames(raw: String): String =
        raw
            .replace(Regex("""\bSept\b""", RegexOption.IGNORE_CASE), "Sep")
            .replace(Regex("""\bJune\b""", RegexOption.IGNORE_CASE), "Jun")
            .replace(Regex("""\bJuly\b""", RegexOption.IGNORE_CASE), "Jul")

    fun atStartOfDay(date: LocalDate, zone: ZoneId): java.time.Instant =
        date.atTime(LocalTime.MIDNIGHT).atZone(zone).toInstant()
}
