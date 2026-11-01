package com.kuliahku.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatters {
    private val localeId = Locale("id", "ID")

    private val fullDate = SimpleDateFormat("EEEE dd/MM/yyyy", localeId)
    private val mediumDate = SimpleDateFormat("EEEE, dd MMM yyyy", localeId)
    private val dateTime = SimpleDateFormat("dd MMMM yyyy HH.mm", localeId)
    private val timeOnly = SimpleDateFormat("HH.mm", localeId)
    private val monthYear = SimpleDateFormat("MMMM yyyy", localeId)
    private val dayLong = SimpleDateFormat("d MMMM yyyy", localeId)

    fun formatFullDate(d: Date): String = fullDate.format(d)
    fun formatMediumDate(d: Date): String = mediumDate.format(d)
    fun formatDateTime(d: Date): String = dateTime.format(d)
    fun formatTime(d: Date): String = timeOnly.format(d)
    fun formatMonthYear(d: Date): String = monthYear.format(d)
    fun formatDayLong(d: Date): String = dayLong.format(d)

    /** "Pagi/Siang/Sore/Malam" berdasarkan jam perangkat. */
    fun greeting(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (h) {
            in 4..10 -> "Pagi"
            in 11..14 -> "Siang"
            in 15..18 -> "Sore"
            else -> "Malam"
        }
    }
}