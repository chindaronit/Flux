package com.flux.data.model

import android.content.Context
import android.content.Intent
import com.flux.other.ReminderReceiver
import kotlinx.serialization.json.Json

enum class ReminderType { EVENT, HABIT }

data class ScheduleRequest(
    val itemId: String,
    val itemType: ReminderType,
    val title: String,
    val description: String,
    val recurrence: RecurrenceRule,
    val startDateTime: Long,
    val endDateTime: Long,
    val notificationOffset: Long,
    val workspaceId: String,
    val habitConfig: HabitConfig? = null
) {
    companion object {
        fun fromIntent(intent: Intent): ScheduleRequest? {
            return try {
                ScheduleRequest(
                    itemId = intent.getStringExtra("itemId") ?: return null,
                    itemType = ReminderType.valueOf(
                        intent.getStringExtra("itemType") ?: return null
                    ),
                    title = intent.getStringExtra("title") ?: "",
                    description = intent.getStringExtra("description") ?: "",
                    recurrence = Json.decodeFromString(
                        intent.getStringExtra("recurrence") ?: return null
                    ),
                    startDateTime = intent.getLongExtra("startDateTime", -1),
                    endDateTime = intent.getLongExtra("endDateTime", -1),
                    notificationOffset = intent.getLongExtra("notificationOffset", 0),
                    workspaceId = intent.getStringExtra("workspaceId") ?: "",
                    habitConfig = intent.getStringExtra("habitConfig")
                        ?.let { Json.decodeFromString<HabitConfig>(it) }
                )
            } catch (_: Exception) { null }
        }
    }
}

private const val DAY_MILLIS =
    24L * 60L * 60L * 1000L

private fun Int.minutesToMillis(): Long {
    return this * 60L * 1000L
}

fun HabitModel.toScheduleRequest() = ScheduleRequest(
    itemId = id,
    itemType = ReminderType.HABIT,
    title = title,
    description = description,
    recurrence = recurrence,
    startDateTime = startDateTime,
    endDateTime = endDateTime,
    notificationOffset = notificationOffset,
    workspaceId = workspaceId,
    habitConfig = habitConfig
)

fun EventModel.toScheduleRequest() = ScheduleRequest(
    itemId = id,
    itemType = ReminderType.EVENT,
    title = title,
    description = description,
    recurrence = recurrence,
    startDateTime = startDateTime,
    endDateTime = endDateTime,
    notificationOffset = notificationOffset,
    workspaceId = workspaceId
)

fun ScheduleRequest.toIntent(context: Context): Intent {
    return Intent(context, ReminderReceiver::class.java).apply {
        putExtra("itemId", itemId)
        putExtra("itemType", itemType.name)
        putExtra("title", title)
        putExtra("description", description)
        putExtra("recurrence", Json.encodeToString(recurrence))
        putExtra("startDateTime", startDateTime)
        putExtra("endDateTime", endDateTime)
        putExtra("notificationOffset", notificationOffset)
        putExtra("workspaceId", workspaceId)
        habitConfig?.let { putExtra("habitConfig", Json.encodeToString(it)) }
    }
}