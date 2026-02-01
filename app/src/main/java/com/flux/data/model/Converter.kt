package com.flux.data.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.json.Json
import java.util.UUID

class Converter {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @TypeConverter
    fun fromRecurrence(rule: RecurrenceRule?): String =
        json.encodeToString(RecurrenceRule.serializer(), rule ?: RecurrenceRule.Once)

    @TypeConverter
    fun toRecurrence(data: String?): RecurrenceRule =
        try {
            if (data.isNullOrBlank()) RecurrenceRule.Once
            else json.decodeFromString(RecurrenceRule.serializer(), data)
        } catch (_: Exception) {
            RecurrenceRule.Once
        }

    @TypeConverter
    fun fromTodoItemList(items: List<TodoItem>): String {
        return Gson().toJson(items)
    }

    @TypeConverter
    fun toTodoItemList(json: String): List<TodoItem> {
        return try {
            val type = object : TypeToken<List<TodoItem>>() {}.type
            val items: List<TodoItem> = Gson().fromJson(json, type)

            // Ensure all items have valid IDs (migration for old data)
            items.map { item ->
                if (item.id.isEmpty()) {
                    item.copy(id = UUID.randomUUID().toString())
                } else {
                    item
                }
            }
        } catch (_: Exception) {
            // Fallback: Try to parse old format without id field
            try {
                val type = object : TypeToken<List<OldTodoItem>>() {}.type
                val oldItems: List<OldTodoItem> = Gson().fromJson(json, type)
                oldItems.map { oldItem ->
                    TodoItem(
                        id = UUID.randomUUID().toString(),
                        value = oldItem.value,
                        isChecked = oldItem.isChecked
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return value.joinToString(separator = ",")
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        return if (value.isEmpty()) emptyList()
        else value.split(",").map { it.toInt() }
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }
}

// Helper class for parsing old data format (without id field)
private data class OldTodoItem(
    var value: String = "",
    var isChecked: Boolean = false
)