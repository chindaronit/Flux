package com.flux.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Serializable
@Entity
data class EventModel(
    @PrimaryKey
    override val id: String = UUID.randomUUID().toString(),
    override val title: String = "",
    override val description: String = "",
    override val recurrence: RecurrenceRule = RecurrenceRule.Custom(),
    override val startDateTime: Long = System.currentTimeMillis(),
    override val endDateTime: Long = -1L,
    override val notificationOffset: Long = 0L,
    override val workspaceId: String = ""
) : ReminderItem {
    override val type: ReminderType get() = ReminderType.EVENT
}

@Serializable
@Entity(primaryKeys = ["eventId", "instanceDate"])
data class EventInstanceModel(
    val eventId: String = "",
    val workspaceId: String = "",
    val instanceDate: Long = LocalDate.now().toEpochDay()
)

fun EventModel.occursOn(date: LocalDate): Boolean {
    val eventStart = Instant.ofEpochMilli(startDateTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    // If endDateTime is set and the event ended before this date → skip it
    if (endDateTime > 0) {
        val eventEnd = Instant.ofEpochMilli(endDateTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        if (date.isAfter(eventEnd)) return false
    }

    return when (val r = recurrence) {
        is RecurrenceRule.Once -> eventStart == date

        is RecurrenceRule.Custom -> {
            val daysSinceStart = ChronoUnit.DAYS.between(eventStart, date)
            daysSinceStart >= 0 && daysSinceStart % r.everyXDays == 0L
        }

        is RecurrenceRule.Weekly -> {
            val dayOfWeek = (date.dayOfWeek.value + 6) % 7
            date >= eventStart && r.daysOfWeek.contains(dayOfWeek)
        }

        is RecurrenceRule.Monthly -> {
            if (date < eventStart) return false
            val lastDayOfMonth = date.lengthOfMonth()
            val dayToCheck = minOf(eventStart.dayOfMonth, lastDayOfMonth)
            date.dayOfMonth == dayToCheck
        }

        is RecurrenceRule.Yearly -> {
            date >= eventStart &&
                    date.dayOfMonth == eventStart.dayOfMonth &&
                    date.month == eventStart.month
        }
    }
}