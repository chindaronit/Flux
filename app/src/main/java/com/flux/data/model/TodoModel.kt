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

fun TodoModel.toHtml(): String {
    val itemsHtml = items.joinToString("\n") { item ->
        val checkbox = if (item.isChecked) "☑" else "☐"

        """
        <div class="todo-item">
            <span class="checkbox">$checkbox</span>
            <span>${item.value}</span>
        </div>
        """.trimIndent()
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body {
                    font-family: sans-serif;
                    padding: 32px;
                    color: black;
                    background: white;
                }

                h1 {
                    margin-bottom: 24px;
                }

                .todo-item {
                    display: flex;
                    align-items: center;
                    margin-bottom: 16px;
                    font-size: 18px;
                }

                .checkbox {
                    margin-right: 12px;
                }
            </style>
        </head>
        <body>
            <h1>$title</h1>

            $itemsHtml

        </body>
        </html>
    """.trimIndent()
}

fun TodoInstance.toHtml(title: String): String {
    val itemsHtml = items.joinToString("\n") { item ->
        val checkbox = if (item.isChecked) "☑" else "☐"

        """
        <div class="todo-item">
            <span class="checkbox">$checkbox</span>
            <span class="text">${item.value}</span>
        </div>
        """.trimIndent()
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8"/>
            <style>
                html {
                    width: max-content;
                    height: max-content;
                }

                body {
                    display: inline-block;
                    width: max-content;
                    margin: 0;
                    padding: 32px;
                    background: white;
                    color: black;
                    font-family: sans-serif;
                }

                h1 {
                    margin: 0 0 24px 0;
                    font-size: 28px;
                    font-weight: bold;
                    white-space: nowrap;
                }

                .todo-item {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    margin-bottom: 16px;
                    font-size: 18px;
                    line-height: 1.5;
                    white-space: nowrap;
                }

                .checkbox {
                    flex-shrink: 0;
                    font-size: 20px;
                }

                .text {
                    flex: 1;
                }
            </style>
        </head>
        <body>

            <h1>$title</h1>

            $itemsHtml

        </body>
        </html>
    """.trimIndent()
}

// Title is excluded
fun TodoModel.toMarkdownContent(): String {
    return buildString {
        items.forEach { item ->
            val check = if (item.isChecked) "x" else " "
            append("- [$check] ${item.value}\n")
        }
    }
}


fun TodoModel.toMarkdown(): String {
    return buildString {
        append("# $title\n\n")

        items.forEach { item ->
            val check = if (item.isChecked) "x" else " "
            append("- [$check] ${item.value}\n")
        }
    }
}

fun TodoModel.toText(): String {
    return buildString {
        append("# $title\n\n")

        items.forEach { item ->
            val check = if (item.isChecked) "☑" else "☐"
            append("- $check ${item.value}\n")
        }
    }
}