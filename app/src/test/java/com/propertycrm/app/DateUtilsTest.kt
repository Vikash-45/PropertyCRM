package com.propertycrm.app

import com.propertycrm.app.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateUtilsTest {

    @Test
    fun testStartAndEndOfDay() {
        val calendar = Calendar.getInstance()
        calendar.set(2026, Calendar.NOVEMBER, 6, 14, 30, 45)
        val timestamp = calendar.timeInMillis

        val startOfDay = DateUtils.getStartOfDay(timestamp)
        val endOfDay = DateUtils.getEndOfDay(timestamp)

        val calStart = Calendar.getInstance().apply { timeInMillis = startOfDay }
        assertEquals(0, calStart.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calStart.get(Calendar.MINUTE))
        assertEquals(0, calStart.get(Calendar.SECOND))
        assertEquals(0, calStart.get(Calendar.MILLISECOND))

        val calEnd = Calendar.getInstance().apply { timeInMillis = endOfDay }
        assertEquals(23, calEnd.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calEnd.get(Calendar.MINUTE))
        assertEquals(59, calEnd.get(Calendar.SECOND))
        assertEquals(999, calEnd.get(Calendar.MILLISECOND))
    }

    @Test
    fun testIsTodayAndOverdueAndUpcoming() {
        val today = Calendar.getInstance()
        today.set(2026, Calendar.NOVEMBER, 6, 12, 0, 0)
        val now = today.timeInMillis

        // Yesterday
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis

        // Tomorrow
        val tomorrow = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        // Today checks
        assertTrue("Now should be today", DateUtils.isToday(now, now))
        assertFalse("Yesterday is not today", DateUtils.isToday(yesterday, now))
        assertFalse("Tomorrow is not today", DateUtils.isToday(tomorrow, now))

        // Overdue checks
        assertTrue("Yesterday should be overdue", DateUtils.isOverdue(yesterday, now))
        assertFalse("Today should not be overdue", DateUtils.isOverdue(now, now))
        assertFalse("Tomorrow should not be overdue", DateUtils.isOverdue(tomorrow, now))

        // Upcoming checks
        assertTrue("Tomorrow should be upcoming", DateUtils.isUpcoming(tomorrow, now))
        assertFalse("Today should not be upcoming", DateUtils.isUpcoming(now, now))
        assertFalse("Yesterday should not be upcoming", DateUtils.isUpcoming(yesterday, now))
    }

    @Test
    fun testCalculateNextReminderTime() {
        val calendar = Calendar.getInstance()
        calendar.set(2026, Calendar.NOVEMBER, 6, 8, 0, 0) // 8:00 AM
        val morningTime = calendar.timeInMillis

        // Schedule for 9:00 AM -> should be scheduled today
        val nextReminderToday = DateUtils.calculateNextReminderTime(9, 0, morningTime)
        val calScheduledToday = Calendar.getInstance().apply { timeInMillis = nextReminderToday }
        assertEquals(9, calScheduledToday.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calScheduledToday.get(Calendar.MINUTE))
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), calScheduledToday.get(Calendar.DAY_OF_MONTH))

        // Current time: 10:00 AM -> schedule for 9:00 AM -> should be scheduled for tomorrow
        calendar.set(Calendar.HOUR_OF_DAY, 10)
        val lateTime = calendar.timeInMillis
        val nextReminderTomorrow = DateUtils.calculateNextReminderTime(9, 0, lateTime)
        val calScheduledTomorrow = Calendar.getInstance().apply { timeInMillis = nextReminderTomorrow }
        assertEquals(9, calScheduledTomorrow.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calScheduledTomorrow.get(Calendar.MINUTE))
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH) + 1, calScheduledTomorrow.get(Calendar.DAY_OF_MONTH))
    }
}
