package com.shashanksoni.kharchahogayabhai.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Date and time strings for the UI. Kept out of composables so screens do not
 * grow formatting logic, and so the "today"/"yesterday" wording can be tested by
 * passing a fixed date.
 */
object DateTimeFormatters {

    private val MonthAndYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val ShortMonthAndYear = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
    private val DayAndMonth = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
    private val DayMonthAndYear = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
    private val TimeOfDay = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val DayOfMonth = DateTimeFormatter.ofPattern("d", Locale.getDefault())

    /** e.g. `September 2026`. */
    fun monthLabel(month: YearMonth): String = MonthAndYear.format(month)

    /** e.g. `Sep 2026`, for tight spots like comparison labels. */
    fun shortMonthLabel(month: YearMonth): String = ShortMonthAndYear.format(month)

    /** Section header for a day's transactions: `Today`, `Yesterday`, or `17 September`. */
    fun dayHeader(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> if (date.year == today.year) DayAndMonth.format(date) else DayMonthAndYear.format(date)
    }

    /** e.g. `17 September 2026`. */
    fun fullDate(instant: Instant, zone: ZoneId): String =
        DayMonthAndYear.format(instant.atZone(zone))

    /** e.g. `3:14 PM`. */
    fun timeOfDay(instant: Instant, zone: ZoneId): String = TimeOfDay.format(instant.atZone(zone))

    fun dayOfMonth(date: LocalDate): String = DayOfMonth.format(date)

    fun localDateOf(instant: Instant, zone: ZoneId): LocalDate = instant.atZone(zone).toLocalDate()
}
