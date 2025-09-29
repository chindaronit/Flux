package com.flux.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

enum class ReminderType { EVENT, HABIT }

@Serializable
sealed class RecurrenceRule {
    @Serializable data class Once(val atDay: Long = LocalDate.now().toEpochDay()) : RecurrenceRule()
    @Serializable data class Day(val everyXDays: Int = 1) : RecurrenceRule()
    @Serializable data class Week(val daysOfWeek: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6)) : RecurrenceRule()
    @Serializable data class Month(val dayOfMonth: Int = LocalDate.now().dayOfMonth) : RecurrenceRule()
    @Serializable data class Year(val date: Long = LocalDate.now().toEpochDay()) : RecurrenceRule()
}

interface ReminderItem {
    val id: String
    val title: String
    val description: String
    val recurrence: RecurrenceRule
    val type: ReminderType
    val startDateTime: Long
    val notificationOffset: Long
}