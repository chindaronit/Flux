package com.flux.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class HabitConfig {

    @Serializable
    @SerialName("Simple")
    object Simple : HabitConfig()

    @Serializable
    @SerialName("Counted")
    data class Counted(
        val goal: Int = 2,
        val unit: String = "",
        val intervalMillis: Long = 60_000L,      // reminder interval
        val activeStartTime: Long = System.currentTimeMillis(),     // millis of day
        val activeEndTime: Long = System.currentTimeMillis()
    ) : HabitConfig()

    @Serializable
    @SerialName("Timed")
    data class Timed(val durationMillis: Long = 60_000L) : HabitConfig()
}