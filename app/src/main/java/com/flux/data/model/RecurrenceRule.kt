package com.flux.data.model

import kotlinx.serialization.Serializable

enum class ReminderType { EVENT, HABIT }

@Serializable
sealed class RecurrenceRule {
    @Serializable object None : RecurrenceRule()
    @Serializable data class Daily(val everyXDays: Int = 1) : RecurrenceRule()
    @Serializable data class Weekly(val daysOfWeek: List<Int>) : RecurrenceRule()
    @Serializable data class Monthly(val dayOfMonth: Int) : RecurrenceRule()
    @Serializable data class Yearly(val month: Int, val dayOfMonth: Int) : RecurrenceRule()
}

interface ReminderItem {
    val id: String
    val title: String
    val description: String
    val startDateTime: Long
    val recurrence: RecurrenceRule
    val type: ReminderType
}