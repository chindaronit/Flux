package com.flux.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

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
            val daysBetween =
                startDate.until(date).days

            daysBetween >= 0 &&
                    daysBetween % everyXDays == 0
        }
    }
}