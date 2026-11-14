package com.omnipad.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.contentOrNull
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@Serializable
sealed class RecurrenceRule {
    @Serializable
    @SerialName("NONE")
    object NONE: RecurrenceRule()

    @Serializable
    @SerialName("Once")
    object Once : RecurrenceRule()

    @Serializable
    @SerialName("Weekly")
    data class Weekly(val daysOfWeek: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6)) : RecurrenceRule()

    @Serializable
    @SerialName("Monthly")
    object Monthly : RecurrenceRule()

    @Serializable
    @SerialName("Yearly")
    object Yearly : RecurrenceRule()

    @Serializable
    @SerialName("Custom")
    data class Custom(val everyXDays: Int = 1) : RecurrenceRule()

    @Serializable
    @SerialName("Interval")
    data class Interval(
        val unit: IntervalUnit,
        val every: Int = 1,
        val daysOfWeek: List<Int> = emptyList()
    ) : RecurrenceRule()
}

object RecurrenceRuleCompatSerializer :
    JsonTransformingSerializer<RecurrenceRule>(RecurrenceRule.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val obj = element as? JsonObject ?: return element
        val type = (obj["type"] as? JsonPrimitive)?.contentOrNull ?: return element
        val prefix = listOf(
            "com.flux.data.model.RecurrenceRule.",
            "com.omnipad.data.model.RecurrenceRule."
        ).firstOrNull { type.startsWith(it) } ?: return element
        return JsonObject(obj + ("type" to JsonPrimitive(type.removePrefix(prefix))))
    }
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