package com.flux.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

@Serializable
@Entity(indices = [Index("workspaceId"), Index("endDateTime")])
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
@Entity(primaryKeys = ["habitId", "instanceDate"], indices = [Index("instanceDate")])
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

fun HabitModel.startDateAsLocalDate(zoneId: ZoneId): LocalDate =
    Instant.ofEpochMilli(startDateTime).atZone(zoneId).toLocalDate()

val HabitModel.isTimed get() = habitConfig is HabitConfig.Timed
val HabitModel.isCounted get() = habitConfig is HabitConfig.Counted

fun HabitInstanceModel.isCompleted(habit: HabitModel): Boolean {
    return when (val config = habit.habitConfig) {
        is HabitConfig.Simple -> true

        is HabitConfig.Counted -> count >= config.goal

        is HabitConfig.Timed -> timeSpent >= config.durationMillis
    }
}

data class HabitAchievementStats(
    val habitTitle: String,
    val habitDescription: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val consistencyPercent: Int,
    val totalCompletions: Int,
    val heatMapData: Map<LocalDate, Int>
)

data class HabitWithStatus(val habit: HabitModel, val isCompleted: Boolean)