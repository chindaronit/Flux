package com.flux.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed class RecurrenceRule {
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