package com.flux.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit

@Serializable
@Entity
data class EventModel(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val recurrence: RecurrenceRule = RecurrenceRule.Once,
    val startDateTime: Long = System.currentTimeMillis(),
    val endDateTime: Long = -1L,
    val notificationOffset: Long = 0L,
    // Null means this event predates multiple reminders and should use notificationOffset.
    // An empty list explicitly means that no reminders are configured.
    val notificationOffsets: List<Long>? = null,
    val workspaceId: String = "",
    val colorArgb: Int? = null,
    val attachments: List<String> = emptyList()
)

fun EventModel.effectiveReminderOffsets(): List<Long> =
    (notificationOffsets ?: listOf(notificationOffset)).distinct()

@Serializable
@Entity(primaryKeys = ["eventId", "instanceDate"])
data class EventInstanceModel(
    val eventId: String = "",
    val workspaceId: String = "",
    val instanceDate: Long = LocalDate.now().toEpochDay()
)

@Serializable
@Entity(primaryKeys = ["eventId", "occurrenceDate"])
data class EventExceptionModel(
    val eventId: String,
    val occurrenceDate: Long,
    val workspaceId: String
)

@Serializable
@Entity(primaryKeys = ["eventId", "occurrenceDate"])
data class EventOccurrenceColorModel(
    val eventId: String,
    val workspaceId: String,
    val occurrenceDate: Long,
    val colorArgb: Int
)

data class EventOccurrence(
    val event: EventModel,
    val date: LocalDate
) {
    val instanceDate: Long get() = date.toEpochDay()
}

fun findEventOccurrences(
    events: List<EventModel>,
    exceptions: List<EventExceptionModel>,
    fromDate: LocalDate,
    throughDate: LocalDate
): List<EventOccurrence> {
    if (throughDate.isBefore(fromDate)) return emptyList()
    val excludedOccurrences = exceptions
        .groupBy { it.eventId }
        .mapValues { (_, entries) -> entries.map { it.occurrenceDate }.toHashSet() }

    return buildList {
        events.forEach { event ->
            val eventStart = Instant.ofEpochMilli(event.startDateTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            if (event.recurrence is RecurrenceRule.Once) {
                if (eventStart.toEpochDay() !in excludedOccurrences[event.id].orEmpty()) {
                    add(EventOccurrence(event, eventStart))
                }
                return@forEach
            }
            val eventEnd = event.endDateTime.takeIf { it > 0L }
                ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            var date = maxOf(eventStart, fromDate)
            val lastDate = minOf(throughDate, eventEnd ?: throughDate)
            while (!date.isAfter(lastDate)) {
                if (event.occursOn(date) && date.toEpochDay() !in excludedOccurrences[event.id].orEmpty()) {
                    add(EventOccurrence(event, date))
                }
                date = date.plusDays(1)
            }
        }
    }.sortedWith { first, second ->
        first.date.compareTo(second.date)
            .takeIf { it != 0 }
            ?: first.event.startDateTime.compareTo(second.event.startDateTime)
                .takeIf { it != 0 }
            ?: first.event.title.compareTo(second.event.title, ignoreCase = true)
                .takeIf { it != 0 }
            ?: first.event.id.compareTo(second.event.id)
    }
}

fun initialEventOccurrenceIndex(
    occurrences: List<EventOccurrence>,
    today: LocalDate = LocalDate.now()
): Int {
    val todayOrFuture = occurrences.indexOfFirst { !it.date.isBefore(today) }
    return when {
        todayOrFuture >= 0 -> todayOrFuture
        occurrences.isNotEmpty() -> occurrences.lastIndex
        else -> 0
    }
}

fun EventModel.isLive(): Boolean {
    if (endDateTime == -1L) return true
    return endDateTime > System.currentTimeMillis()
}

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
            daysSinceStart >= 0 && daysSinceStart % r.everyXDays.coerceAtLeast(1) == 0L
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

        is RecurrenceRule.Interval -> {
            val interval = r.every.coerceAtLeast(1)
            when (r.unit) {
                IntervalUnit.DAY -> {
                    val daysSinceStart = ChronoUnit.DAYS.between(eventStart, date)
                    daysSinceStart >= 0 && daysSinceStart % interval == 0L
                }
                IntervalUnit.WEEK -> {
                    val selectedDays = r.daysOfWeek.ifEmpty { listOf((eventStart.dayOfWeek.value + 6) % 7) }
                    val startWeek = eventStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val dateWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val weeksSinceStart = ChronoUnit.WEEKS.between(startWeek, dateWeek)
                    weeksSinceStart >= 0 &&
                        weeksSinceStart % interval == 0L &&
                        ((date.dayOfWeek.value + 6) % 7) in selectedDays
                }
                IntervalUnit.MONTH -> {
                    val monthsSinceStart = ChronoUnit.MONTHS.between(
                        eventStart.withDayOfMonth(1),
                        date.withDayOfMonth(1)
                    )
                    val dayOfMonth = minOf(eventStart.dayOfMonth, date.lengthOfMonth())
                    monthsSinceStart >= 0 &&
                        monthsSinceStart % interval == 0L &&
                        date.dayOfMonth == dayOfMonth
                }
                IntervalUnit.YEAR -> {
                    val yearsSinceStart = date.year - eventStart.year
                    yearsSinceStart >= 0 &&
                        yearsSinceStart % interval == 0 &&
                        date.month == eventStart.month &&
                        date.dayOfMonth == minOf(eventStart.dayOfMonth, date.lengthOfMonth())
                }
            }
        }

        else -> false
    }
}