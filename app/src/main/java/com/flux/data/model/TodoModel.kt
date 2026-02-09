package com.flux.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class TodoModel(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val workspaceId: String ="",
    val title: String = "",
    val items: List<TodoItem> = emptyList()
)

@Serializable
data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val value: String = "",
    val isChecked: Boolean = false
)