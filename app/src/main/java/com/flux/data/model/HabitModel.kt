package com.flux.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class HabitModel(
    @PrimaryKey
    override val id: String = UUID.randomUUID().toString(),
    override val title: String = "",
    override val description: String = "",
    override val recurrence: RecurrenceRule = RecurrenceRule.Weekly(),
    override val startDateTime: Long = System.currentTimeMillis(),
    override val endDateTime: Long = -1L,
    override val notificationOffset: Long = 0L,
    val workspaceId: String = "",
    val bestStreak: Long = 0L
) : ReminderItem {
    override val type: ReminderType get() = ReminderType.HABIT
}

@Serializable
@Entity(primaryKeys = ["habitId", "instanceDate"])
data class HabitInstanceModel(
    val habitId: String = "",
    val workspaceId: String = "",
    val instanceDate: Long = LocalDate.now().toEpochDay()
)

fun HabitModel.isLive(): Boolean {
    if (endDateTime == -1L) return true
    return endDateTime > System.currentTimeMillis()
}