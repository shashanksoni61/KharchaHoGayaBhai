package com.shashanksoni.kharchahogayabhai.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class StatementDateParserTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun `parses PhonePe date and time columns`() {
        val instant = StatementDateParser.parseDateAndTime("Sept 17, 2026", "09:51 pm", zone)
        assertNotNull(instant)
        val local = instant!!.atZone(zone)
        assertEquals(LocalDate.of(2026, 9, 17), local.toLocalDate())
        assertEquals(LocalTime.of(21, 51), local.toLocalTime())
    }

    @Test
    fun `parses Aug abbreviated PhonePe dates`() {
        val date = StatementDateParser.parseDate("Aug 18, 2026")
        assertEquals(LocalDate.of(2026, 8, 18), date)
    }
}
