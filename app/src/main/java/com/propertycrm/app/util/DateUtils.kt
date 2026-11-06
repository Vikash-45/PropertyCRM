package com.propertycrm.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    private const val DATE_FORMAT = "dd MMM yyyy"
    private const val DATE_FORMAT_INPUT = "yyyy-MM-dd"
    private const val TIME_FORMAT_12H = "hh:mm a"

    /**
     * Returns the timestamp (in millis) representing 00:00:00.000 of the specified time (or now).
     */
    fun getStartOfDay(timeMillis: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Returns the timestamp (in millis) representing 23:59:59.999 of the specified time (or now).
     */
    fun getEndOfDay(timeMillis: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * Formats epoch millis into readable date, e.g. "06 Nov 2026"
     */
    fun formatDate(timeMillis: Long?): String {
        if (timeMillis == null || timeMillis <= 0L) return "-"
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return sdf.format(Date(timeMillis))
    }

    /**
     * Formats epoch millis into ISO date string "yyyy-MM-dd" (for CSV and JSON)
     */
    fun formatDateIso(timeMillis: Long?): String {
        if (timeMillis == null || timeMillis <= 0L) return ""
        val sdf = SimpleDateFormat(DATE_FORMAT_INPUT, Locale.US)
        return sdf.format(Date(timeMillis))
    }

    /**
     * Parses ISO date string "yyyy-MM-dd" back to start of day epoch millis.
     */
    fun parseDateIso(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            val sdf = SimpleDateFormat(DATE_FORMAT_INPUT, Locale.US)
            val date = sdf.parse(dateStr.trim())
            date?.time?.let { getStartOfDay(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats hour and minute into "09:00 AM"
     */
    fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        val sdf = SimpleDateFormat(TIME_FORMAT_12H, Locale.getDefault())
        return sdf.format(calendar.time)
    }

    /**
     * Checks if a follow-up date is overdue (strictly before today's start of day).
     */
    fun isOverdue(followUpDateMillis: Long, currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val todayStart = getStartOfDay(currentTimeMillis)
        return followUpDateMillis < todayStart
    }

    /**
     * Checks if a follow-up date falls on today (between start and end of today).
     */
    fun isToday(followUpDateMillis: Long, currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val todayStart = getStartOfDay(currentTimeMillis)
        val todayEnd = getEndOfDay(currentTimeMillis)
        return followUpDateMillis in todayStart..todayEnd
    }

    /**
     * Checks if a follow-up date is upcoming (strictly after today's end of day).
     */
    fun isUpcoming(followUpDateMillis: Long, currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val todayEnd = getEndOfDay(currentTimeMillis)
        return followUpDateMillis > todayEnd
    }

    /**
     * Computes the trigger time in epoch millis for the next daily reminder.
     * If the time today has already passed, schedules for tomorrow at the same time.
     */
    fun calculateNextReminderTime(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = nowMillis
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // If today's reminder time has already passed, schedule for tomorrow
        if (calendar.timeInMillis <= nowMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
}
