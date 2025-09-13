package com.flux.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class EventModel(
    @PrimaryKey
    val eventId: String = UUID.randomUUID().toString(),
    val workspaceId: String = "",
    val title: String = "",
    val description: String = "",
    val repetition: String = "NONE",
    val isAllDay: Boolean = false,
    val notificationOffset: Long = 0L,
    val startDateTime: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(primaryKeys = ["eventId", "instanceDate"])
data class EventInstanceModel(
    val eventId: String = "",
    val workspaceId: String = "",
    val instanceDate: Long = LocalDate.now().toEpochDay()
)