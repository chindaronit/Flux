package com.flux.data.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.json.Json

class Converter {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @TypeConverter
    fun fromRecurrence(rule: RecurrenceRule?): String =
        json.encodeToString(RecurrenceRule.serializer(), rule ?: RecurrenceRule.Once())

    @TypeConverter
    fun toRecurrence(data: String?): RecurrenceRule =
        try {
            if (data.isNullOrBlank()) RecurrenceRule.Once()
            else json.decodeFromString(RecurrenceRule.serializer(), data)
        } catch (_: Exception) {
            RecurrenceRule.Once()
        }

    @TypeConverter
    fun fromTodoItemList(items: List<TodoItem>): String {
        return Gson().toJson(items)
    }

    @TypeConverter
    fun toTodoItemList(json: String): List<TodoItem> {
        val type = object : TypeToken<List<TodoItem>>() {}.type
        return Gson().fromJson(json, type)
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
