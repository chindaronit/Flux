package com.flux.data.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class EventModelTest {
    @Test
    fun findEventOccurrencesSortsEachDayByLocalTimeThenTitleAndId() {
        val date = LocalDate.of(2026, 11, 13)
        fun start(day: LocalDate, hour: Int): Long =
            day.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val late = EventModel(
            id = "late",
            startDateTime = start(date.minusDays(10), 17),
            recurrence = RecurrenceRule.Custom()
        )
        val early = EventModel(
            id = "early",
            startDateTime = start(date.minusDays(2), 9),
            recurrence = RecurrenceRule.Custom()
        )
        val sameTimeB = EventModel(id = "b", title = "same", startDateTime = start(date, 9))
        val sameTimeA = EventModel(id = "a", title = "Same", startDateTime = start(date, 9))
        val once = EventModel(id = "once", startDateTime = start(date, 12))

        val occurrences = findEventOccurrences(
            listOf(late, sameTimeB, once, early, sameTimeA),
            emptyList(),
            date,
            date.plusDays(1)
        )

        assertEquals(
            listOf("early", "a", "b", "once", "late", "early", "late"),
            occurrences.map { it.event.id }
        )
        assertEquals(listOf(date, date, date, date, date, date.plusDays(1), date.plusDays(1)),
            occurrences.map { it.date })
    }

    @Test
    fun filterCompletedEventOccurrencesHidesOnlyCompletedInstances() {
        val date = LocalDate.of(2026, 11, 13)
        val completedEvent = EventModel(id = "completed")
        val pendingEvent = EventModel(id = "pending")
        val occurrences = listOf(
            EventOccurrence(completedEvent, date),
            EventOccurrence(pendingEvent, date)
        )
        val instances = listOf(
            EventInstanceModel(
                eventId = completedEvent.id,
                instanceDate = date.toEpochDay()
            )
        )

        assertEquals(
            listOf(occurrences[1]),
            filterCompletedEventOccurrences(occurrences, instances, showCompletedEvents = false)
        )
        assertEquals(
            occurrences,
            filterCompletedEventOccurrences(occurrences, instances, showCompletedEvents = true)
        )
    }

    @Test
    fun filterCompletedEventOccurrencesDistinguishesRecurringDates() {
        val event = EventModel(id = "recurring")
        val completedDate = LocalDate.of(2026, 11, 13)
        val pendingDate = completedDate.plusDays(1)
        val occurrences = listOf(
            EventOccurrence(event, completedDate),
            EventOccurrence(event, pendingDate)
        )
        val instances = listOf(
            EventInstanceModel(
                eventId = event.id,
                instanceDate = completedDate.toEpochDay()
            )
        )

        assertEquals(
            listOf(occurrences[1]),
            filterCompletedEventOccurrences(occurrences, instances, showCompletedEvents = false)
        )
    }
}
