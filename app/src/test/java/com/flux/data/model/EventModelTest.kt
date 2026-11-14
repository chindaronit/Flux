package com.flux.data.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EventModelTest {
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
