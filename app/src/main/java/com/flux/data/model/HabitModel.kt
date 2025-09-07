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
    val habitId: String = UUID.randomUUID().toString(),
    val workspaceId: String = "",
    val title: String = "",
    val description: String = "",
    val bestStreak: Long = 0L,
    val startDateTime: Long = System.currentTimeMillis()
)
@Serializable
@Entity(primaryKeys = ["habitId", "instanceDate"])
data class HabitInstanceModel(
    val habitId: String = "",
    val workspaceId: String = "",
    val instanceDate: Long = LocalDate.now().toEpochDay()
)