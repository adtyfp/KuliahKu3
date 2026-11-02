package com.kuliahku.util

import java.util.Calendar
import java.util.Date
import java.util.Locale

object MonthKey {
    private val monthFmt = java.text.SimpleDateFormat("yyyy-MM", Locale.US)
    private val yearFmt = java.text.SimpleDateFormat("yyyy", Locale.US)

    fun of(date: Date): String = monthFmt.format(date)
    fun yearOf(date: Date): String = yearFmt.format(date)
    fun of(cal: Calendar): String = monthFmt.format(cal.time)

    /** "1-6", "7-13", "14-20", "21-27", "28-31" */
    fun weekBucket(dayOfMonth: Int): String = when (dayOfMonth) {
        in 1..6 -> "1-6"
        in 7..13 -> "7-13"
        in 14..20 -> "14-20"
        in 21..27 -> "21-27"
        else -> "28-31"
    }

    val BUCKETS = listOf("1-6", "7-13", "14-20", "21-27", "28-31")

    fun currentBucket(): String = weekBucket(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
}