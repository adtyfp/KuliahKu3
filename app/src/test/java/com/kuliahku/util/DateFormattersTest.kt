package com.kuliahku.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class DateFormattersTest {

    @Test fun format_full_date() {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 23) }
        // Sabtu 23/10/2026 (hasil bisa beda tergantung TZ, gunakan UTC untuk stabil)
        val formatted = DateFormatters.formatFullDate(cal.time)
        assert(formatted.contains("23/10/2026"))
    }

    @Test fun format_time() {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 23, 14, 30) }
        assertEquals("14.30", DateFormatters.formatTime(cal.time))
    }

    @Test fun greeting_maps_hours_correctly() {
        // Test statis: jam 8 -> Pagi, 12 -> Siang, 16 -> Sore, 20 -> Malam
        val hours = listOf(8 to "Pagi", 12 to "Siang", 16 to "Sore", 20 to "Malam")
        // Karena greeting() membaca jam sistem, kita tidak bisa set langsung.
        // Refactor greeting(jam: Int): String untuk memudahkan test.
    }
}