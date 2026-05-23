package com.flux.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class HabitModel(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val recurrence: RecurrenceRule = RecurrenceRule.Weekly(),
    val startDateTime: Long = System.currentTimeMillis(),
    val endDateTime: Long = -1L,
    val notificationOffset: Long = 0L,
    val workspaceId: String = "",
    val habitConfig: HabitConfig = HabitConfig.Simple
)

@Serializable
@Entity(primaryKeys = ["habitId", "instanceDate"])
data class HabitInstanceModel(
    val habitId: String = "",
    val workspaceId: String = "",
    val instanceDate: Long = LocalDate.now().toEpochDay(),

    // timed
    val timeSpent: Long = 0L,
    val isRunning: Boolean = false,

    // counted
    val count: Int = 0
)

fun HabitModel.isLive(): Boolean {
    if (endDateTime == -1L) return true
    return endDateTime > System.currentTimeMillis()
}

val HabitModel.isTimed get() = habitConfig is HabitConfig.Timed
val HabitModel.isCounted get() = habitConfig is HabitConfig.Counted

fun HabitInstanceModel.isCompleted(habit: HabitModel): Boolean {
    return when (val config = habit.habitConfig) {
        is HabitConfig.Simple -> true

        is HabitConfig.Counted -> count >= config.goal

        is HabitConfig.Timed -> timeSpent >= config.durationMillis
    }
}