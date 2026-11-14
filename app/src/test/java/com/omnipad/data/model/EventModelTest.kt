package com.omnipad.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class EventModelTest {
    @Test
    fun occurrenceColorOverridesSeriesOnlyForMatchingEventAndDate() {
        val event = EventModel(id = "event", colorArgb = 1)
        val date = LocalDate.of(2026, 11, 13).toEpochDay()
        val colors = listOf(
            EventOccurrenceColorModel("other", "", date, 2),
            EventOccurrenceColorModel(event.id, "", date + 1, 3),
            EventOccurrenceColorModel(event.id, "", date, 4)
        )

        assertEquals(4, eventColorForOccurrence(event, date, colors))
        assertEquals(3, eventColorForOccurrence(event, date + 1, colors))
        assertEquals(1, eventColorForOccurrence(event, date - 1, colors))
        assertNull(eventColorForOccurrence(event.copy(colorArgb = null), date - 1, colors))
    }

    @Test
    fun editorPreviewUsesNewDefaultsOnlyForNewEventsAndMatchesCompletedColors() {
        fun preview(
            selected: Int?,
            series: Int? = null,
            occurrence: Boolean = false,
            isNew: Boolean = false,
            isPending: Boolean = true
        ) = eventEditorPreviewColor(selected, series, occurrence, isNew, 1, 2, isPending)

        assertEquals(1, preview(selected = null, isNew = true))
        assertEquals(3, preview(selected = 3, isNew = true))
        assertNull(preview(selected = null))
        assertEquals(4, preview(selected = 4, series = 3, occurrence = true))
        assertEquals(3, preview(selected = null, series = 3, occurrence = true))
        assertEquals(2, preview(selected = null, isPending = false))
        assertEquals(2, preview(selected = 4, isPending = false))
        assertEquals(displayedEventColor(4, 2, false), preview(selected = 4, isPending = false))
    }

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
