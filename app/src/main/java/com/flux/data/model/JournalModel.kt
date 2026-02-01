package com.flux.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Serializable
@Entity
data class JournalModel(
    @PrimaryKey
    val journalId: String = UUID.randomUUID().toString(),
    val workspaceId: String = "",
    val text: String = "",
    val dateTime: Long = System.currentTimeMillis()
)

fun JournalModel.writtenOnDate(date: LocalDate): Boolean {
    val journalDate = Instant.ofEpochMilli(dateTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    return journalDate == date
}