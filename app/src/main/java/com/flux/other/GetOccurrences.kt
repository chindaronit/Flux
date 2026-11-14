package com.flux.other

import android.text.format.DateUtils
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.IntervalUnit
import com.flux.data.model.isActiveOn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

/**
 * Finds the first occurrence strictly after [afterDateTime], preserving the event's
 * local time. Unlike getNextOccurrence(), this can safely advance from a future
 * occurrence (which is needed when scheduling a reminder well before that occurrence).
 */
fun getNextOccurrenceAfter(
    rule: RecurrenceRule,
    startDateTime: Long,
    afterDateTime: Long
): Long? {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(startDateTime).atZone(zone)
    val firstDate = maxOf(
        start.toLocalDate(),
        Instant.ofEpochMilli(afterDateTime).atZone(zone).toLocalDate()
    )

    for (daysFromFirst in 0..366_000) {
        val date = firstDate.plusDays(daysFromFirst.toLong())
        val startDate = start.toLocalDate()
        val isOccurrence = when (rule) {
            RecurrenceRule.Monthly ->
                date.dayOfMonth == minOf(startDate.dayOfMonth, date.lengthOfMonth())
            else -> rule.isActiveOn(date, startDate)
        }
        if (!isOccurrence) continue

        val occurrence = date.atTime(start.toLocalTime()).atZone(zone)
            .toInstant().toEpochMilli()
        if (occurrence > afterDateTime) return occurrence
    }
    return null
}

fun getNextOccurrence(rule: RecurrenceRule, startDateTime: Long): Long? {
    val now = System.currentTimeMillis()

    // If event hasn't started yet, return the start time
    if (startDateTime > now) return startDateTime

    return when (rule) {
        is RecurrenceRule.Once -> {
            // One-time event: only return if it's still in the future
            null  // Already passed since startDateTime <= now
        }

        is RecurrenceRule.Custom -> {
            val daysPassed = ((now - startDateTime) / DateUtils.DAY_IN_MILLIS).toInt()
            val cyclesPassed = daysPassed / rule.everyXDays
            val nextCycle = cyclesPassed + 1
            startDateTime + (nextCycle * rule.everyXDays * DateUtils.DAY_IN_MILLIS)
        }

        is RecurrenceRule.Interval -> findNextIntervalOccurrence(rule, startDateTime, now)

        is RecurrenceRule.Weekly -> {
            findNextWeeklyOccurrence(rule, startDateTime, now)
        }

        is RecurrenceRule.Monthly -> {
            findNextMonthlyOccurrence(startDateTime, now)
        }

        is RecurrenceRule.Yearly -> {
            findNextYearlyOccurrence(startDateTime, now)
        }

        else -> null
    }
}

private fun findNextIntervalOccurrence(
    rule: RecurrenceRule.Interval,
    startDateTime: Long,
    now: Long
): Long? {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(startDateTime).atZone(zone)
    val nowDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val firstDate = maxOf(start.toLocalDate(), nowDate)
    val interval = rule.every.coerceAtLeast(1).toLong()
    val cycleDays = when (rule.unit) {
        IntervalUnit.DAY -> interval
        IntervalUnit.WEEK -> interval * 7
        IntervalUnit.MONTH -> interval * 31
        IntervalUnit.YEAR -> interval * 366
    }
    val searchDays = (cycleDays + 8).coerceAtMost(366_000)

    for (offset in 0..searchDays) {
        val date = firstDate.plusDays(offset)
        if (rule.isActiveOn(date, start.toLocalDate())) {
            val occurrence = date.atTime(start.toLocalTime()).atZone(zone).toInstant().toEpochMilli()
            if (occurrence > now) return occurrence
        }
    }
    return null
}

private fun findNextWeeklyOccurrence(
    rule: RecurrenceRule.Weekly,
    startDateTime: Long,
    now: Long
): Long {
    val startCal = Calendar.getInstance().apply { timeInMillis = startDateTime }
    val targetHour = startCal.get(Calendar.HOUR_OF_DAY)
    val targetMinute = startCal.get(Calendar.MINUTE)
    val targetSecond = startCal.get(Calendar.SECOND)
    val targetMilli = startCal.get(Calendar.MILLISECOND)

    val cal = Calendar.getInstance()

    // Check the next 7 days to find the next occurrence
    for (i in 0..6) {
        cal.timeInMillis = now + i * DateUtils.DAY_IN_MILLIS
        val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday=0, Sunday=6

        if (dayOfWeek in rule.daysOfWeek) {
            // Set the original time-of-day
            cal.set(Calendar.HOUR_OF_DAY, targetHour)
            cal.set(Calendar.MINUTE, targetMinute)
            cal.set(Calendar.SECOND, targetSecond)
            cal.set(Calendar.MILLISECOND, targetMilli)

            if (cal.timeInMillis > now) {
                return cal.timeInMillis
            }
        }
    }

    // If no day found in next 7 days (shouldn't happen if daysOfWeek is not empty)
    // Return next week's first selected day
    for (i in 7..13) {
        cal.timeInMillis = now + i * DateUtils.DAY_IN_MILLIS
        val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

        if (dayOfWeek in rule.daysOfWeek) {
            cal.set(Calendar.HOUR_OF_DAY, targetHour)
            cal.set(Calendar.MINUTE, targetMinute)
            cal.set(Calendar.SECOND, targetSecond)
            cal.set(Calendar.MILLISECOND, targetMilli)
            return cal.timeInMillis
        }
    }

    // Fallback (should never reach here)
    return startDateTime + 7 * DateUtils.DAY_IN_MILLIS
}

private fun findNextMonthlyOccurrence(startDateTime: Long, now: Long): Long {
    val startCal = Calendar.getInstance().apply { timeInMillis = startDateTime }
    val targetDay = startCal.get(Calendar.DAY_OF_MONTH)
    val targetHour = startCal.get(Calendar.HOUR_OF_DAY)
    val targetMinute = startCal.get(Calendar.MINUTE)
    val targetSecond = startCal.get(Calendar.SECOND)
    val targetMilli = startCal.get(Calendar.MILLISECOND)

    val nextCal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, targetHour)
        set(Calendar.MINUTE, targetMinute)
        set(Calendar.SECOND, targetSecond)
        set(Calendar.MILLISECOND, targetMilli)
    }

    // Try current month first
    val maxDayThisMonth = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dayThisMonth = minOf(targetDay, maxDayThisMonth)
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
    val startCal = Calendar.getInstance().apply { timeInMillis = startDateTime }
    val nowCal = Calendar.getInstance().apply { timeInMillis = now }

    // Try this year first
    val thisYear = (startCal.clone() as Calendar).apply {
        set(Calendar.YEAR, nowCal.get(Calendar.YEAR))
    }

    if (thisYear.timeInMillis > now) {
        return thisYear.timeInMillis
    }

    // Otherwise, next year
    return (thisYear.clone() as Calendar).apply {
        add(Calendar.YEAR, 1)
    }.timeInMillis
}