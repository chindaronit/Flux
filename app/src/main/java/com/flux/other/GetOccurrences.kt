package com.flux.other

import android.text.format.DateUtils
import com.flux.data.model.RecurrenceRule
import java.util.Calendar

fun getNextOccurrence(rule: RecurrenceRule, startDateTime: Long): Long? {
    val now = System.currentTimeMillis()

    return when (rule) {
        is RecurrenceRule.Once -> {
            // One-time event: only return if it's still in the future
            if (startDateTime > now) startDateTime else null
        }

        is RecurrenceRule.Day -> {
            if (startDateTime > now) {
                // Start date is still in the future → use it
                startDateTime
            } else {
                val daysPassed = ((now - startDateTime) / DateUtils.DAY_IN_MILLIS).toInt()
                val cyclesPassed = daysPassed / rule.everyXDays
                val nextCycle = cyclesPassed + 1
                startDateTime + (nextCycle * rule.everyXDays * DateUtils.DAY_IN_MILLIS)
            }
        }

        is RecurrenceRule.Week -> {
            // Weekly recurrence: start scanning from today
            findNextWeeklyOccurrence(rule, startDateTime, now)
        }

        is RecurrenceRule.Month -> {
            // Monthly recurrence: compute next occurrence on/after today
            findNextMonthlyOccurrence(startDateTime, now)
        }

        is RecurrenceRule.Year -> {
            // Yearly recurrence: compute next occurrence on/after today
            findNextYearlyOccurrence(startDateTime, now)
        }
    }
}

private fun findNextWeeklyOccurrence(
    rule: RecurrenceRule.Week,
    startDateTime: Long,
    now: Long
): Long {
    if (startDateTime > now) return startDateTime

    val startCal = Calendar.getInstance().apply { timeInMillis = startDateTime }
    val targetHour = startCal.get(Calendar.HOUR_OF_DAY)
    val targetMinute = startCal.get(Calendar.MINUTE)
    val targetSecond = startCal.get(Calendar.SECOND)
    val targetMilli = startCal.get(Calendar.MILLISECOND)

    val cal = Calendar.getInstance()

    // Check the next 2 weeks to cover all possibilities
    for (i in 0..13) {
        cal.timeInMillis = now + i * DateUtils.DAY_IN_MILLIS
        val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday=0 … Sunday=6

        if (dayOfWeek in rule.daysOfWeek) {
            // Directly set the original time-of-day
            cal.set(Calendar.HOUR_OF_DAY, targetHour)
            cal.set(Calendar.MINUTE, targetMinute)
            cal.set(Calendar.SECOND, targetSecond)
            cal.set(Calendar.MILLISECOND, targetMilli)

            if (cal.timeInMillis > now) return cal.timeInMillis
        }
    }

    // Fallback: unlikely to happen
    return startDateTime
}

private fun findNextMonthlyOccurrence(startDateTime: Long, now: Long): Long {
    val startCal = Calendar.getInstance().apply { timeInMillis = startDateTime }
    val targetDay = startCal.get(Calendar.DAY_OF_MONTH)

    val nextCal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, startCal.get(Calendar.HOUR_OF_DAY))
        set(Calendar.MINUTE, startCal.get(Calendar.MINUTE))
        set(Calendar.SECOND, startCal.get(Calendar.SECOND))
        set(Calendar.MILLISECOND, startCal.get(Calendar.MILLISECOND))
    }

    // If target day in current month is still in the future, use it
    val maxDay = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dayThisMonth = minOf(targetDay, maxDay)
    nextCal.set(Calendar.DAY_OF_MONTH, dayThisMonth)

    if (nextCal.timeInMillis > now) {
        return nextCal.timeInMillis
    }

    // Otherwise, move to next month
    nextCal.add(Calendar.MONTH, 1)
    val maxDayNextMonth = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    nextCal.set(Calendar.DAY_OF_MONTH, minOf(targetDay, maxDayNextMonth))
    return nextCal.timeInMillis
}

private fun findNextYearlyOccurrence(startDateTime: Long, now: Long): Long {
    if (startDateTime > now) return startDateTime

    val startCal = Calendar.getInstance().apply { timeInMillis = startDateTime }
    val nowCal = Calendar.getInstance().apply { timeInMillis = now }

    val thisYear = (startCal.clone() as Calendar).apply {
        set(Calendar.YEAR, nowCal.get(Calendar.YEAR))
    }
    if (thisYear.timeInMillis > now) return thisYear.timeInMillis

    return (thisYear.clone() as Calendar).apply {
        add(Calendar.YEAR, 1)
    }.timeInMillis
}
