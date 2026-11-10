package com.flux.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@Serializable
sealed class RecurrenceRule {
    @Serializable
    object NONE: RecurrenceRule()

    @Serializable
    object Once : RecurrenceRule()

    @Serializable
    data class Weekly(val daysOfWeek: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6)) : RecurrenceRule()

    @Serializable
    object Monthly : RecurrenceRule()

    @Serializable
    object Yearly : RecurrenceRule()

    @Serializable
    data class Custom(val everyXDays: Int = 1) : RecurrenceRule()

    @Serializable
    data class Interval(
        val unit: IntervalUnit,
        val every: Int = 1,
        val daysOfWeek: List<Int> = emptyList()
    ) : RecurrenceRule()
}

@Serializable
enum class IntervalUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

fun RecurrenceRule.isActiveOn(
    date: LocalDate,
    startDate: LocalDate
): Boolean {
    if(startDate>date) return false

    return when (this) {
        RecurrenceRule.NONE -> false

        RecurrenceRule.Once ->
            date == startDate

        is RecurrenceRule.Weekly -> {
            val day = date.dayOfWeek.ordinal
            day in daysOfWeek
        }

        RecurrenceRule.Monthly ->
            date.dayOfMonth == startDate.dayOfMonth

        RecurrenceRule.Yearly ->
            date.dayOfMonth == startDate.dayOfMonth &&
                    date.month == startDate.month

        is RecurrenceRule.Custom -> {
            val daysBetween = ChronoUnit.DAYS.between(startDate, date)

            daysBetween >= 0 &&
                    daysBetween % everyXDays.coerceAtLeast(1) == 0L
        }

        is RecurrenceRule.Interval -> {
            val interval = every.coerceAtLeast(1)
            when (unit) {
                IntervalUnit.DAY -> {
                    val daysBetween = ChronoUnit.DAYS.between(startDate, date)
                    daysBetween % interval == 0L
                }
                IntervalUnit.WEEK -> {
                    val selectedDays = daysOfWeek.ifEmpty { listOf(startDate.dayOfWeek.ordinal) }
                    val startWeek = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val dateWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val weeksBetween = ChronoUnit.WEEKS.between(startWeek, dateWeek)
                    weeksBetween >= 0 &&
                        weeksBetween % interval == 0L &&
                        date.dayOfWeek.ordinal in selectedDays
                }
                IntervalUnit.MONTH -> {
                    val monthsBetween =
                        (date.year - startDate.year) * 12 + date.monthValue - startDate.monthValue
                    monthsBetween >= 0 &&
                        monthsBetween % interval == 0 &&
                        date.dayOfMonth == minOf(startDate.dayOfMonth, date.lengthOfMonth())
                }
                IntervalUnit.YEAR -> {
                    val yearsBetween = date.year - startDate.year
                    yearsBetween >= 0 &&
                        yearsBetween % interval == 0 &&
                        date.month == startDate.month &&
                        date.dayOfMonth == minOf(startDate.dayOfMonth, date.lengthOfMonth())
                }
            }
        }
    }
}