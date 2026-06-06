package com.flux.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
@Entity
data class TodoModel(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val workspaceId: String = "",
    val title: String = "",
    val items: List<TodoItem> = emptyList(),
    val startDateTime: Long = System.currentTimeMillis(),
    val recurrence: RecurrenceRule = RecurrenceRule.NONE,
)

@Serializable
data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val value: String = "",
    val isChecked: Boolean = false
)

@Serializable
@Entity(
    foreignKeys = [ForeignKey(
        entity = TodoModel::class,
        parentColumns = ["id"],
        childColumns = ["todoId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(
            value = ["todoId", "instanceDate"],
            unique = true
        )
    ]
)
data class TodoInstance(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val todoId: String = "",
    val workspaceId: String="",
    val instanceDate: Long = LocalDate.now().toEpochDay(),
    val items: List<TodoItem> = emptyList()
)

fun TodoInstance.isCompleted(): Boolean {
    return items.all { it.isChecked }
}