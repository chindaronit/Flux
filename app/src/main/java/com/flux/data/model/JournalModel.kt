package com.flux.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class JournalModel(
    @PrimaryKey
    val journalId: String = UUID.randomUUID().toString(),
    val workspaceId: String = "",
    val text: String = "",
    val dateTime: Long = System.currentTimeMillis(),
    val images: List<String> = emptyList()
)