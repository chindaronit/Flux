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
    override val recurrence: RecurrenceRule = RecurrenceRule.Once(),
    override val startDateTime: Long = System.currentTimeMillis(),
    override val notificationOffset: Long = 0L,
    val workspaceId: String = ""
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
    val eventDay = Instant.ofEpochMilli(startDateTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    return when (val r = recurrence) {
        is RecurrenceRule.Once -> r.atDay == date.toEpochDay()

        is RecurrenceRule.Day -> {
            val daysSinceStart = ChronoUnit.DAYS.between(eventDay, date)
            daysSinceStart >= 0 && daysSinceStart % r.everyXDays == 0L
        }

        is RecurrenceRule.Week -> {
            val dayOfWeek = (date.dayOfWeek.value + 6) % 7 // Monday=0, Sunday=6
            date >= eventDay && r.daysOfWeek.contains(dayOfWeek)
        }

        is RecurrenceRule.Month -> {
            val day = r.dayOfMonth // 1..31
            val startMonth = eventDay.withDayOfMonth(1)
            val currentMonth = date.withDayOfMonth(1)

            val monthsSinceStart = ChronoUnit.MONTHS.between(startMonth, currentMonth)
            val lastDayOfMonth = date.lengthOfMonth()
            val dayToCheck = minOf(day, lastDayOfMonth)

            monthsSinceStart >= 0 && date.dayOfMonth == dayToCheck
        }

        is RecurrenceRule.Year -> {
            val startDate = eventDay
            date >= startDate &&
                    date.dayOfMonth == startDate.dayOfMonth &&
                    date.month == startDate.month
        }
    }
}