package com.flux.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class NotesModel(
    @PrimaryKey
    val notesId: String = UUID.randomUUID().toString(),
    val workspaceId: String = "",
    val title: String = "",
    val description: String = "",
    val isPinned: Boolean = false,
    val labels: List<String> = emptyList(),
    val lastEdited: Long = System.currentTimeMillis()
)
