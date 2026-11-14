package com.omnipad.ui.screens.events

import com.omnipad.data.model.EventModel
import com.omnipad.data.model.EventOccurrence
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventAgendaTest {
    private val today = LocalDate.of(2026, 11, 14)
    private val event = EventModel(id = "event")

    @Test
    fun todayTargetsDateHeaderAfterEarlierGroups() {
        val occurrences = listOf(
            EventOccurrence(event, today.minusDays(1)),
            EventOccurrence(event, today.minusDays(1)),
            EventOccurrence(event, today),
            EventOccurrence(event, today),
            EventOccurrence(event, today.plusDays(1))
        )

        assertEquals(3, agendaTodayHeaderIndex(occurrences, today))
    }

    @Test
    fun todayFallsBackToNextDateOrLastPastDate() {
        val occurrences = listOf(
            EventOccurrence(event, today.minusDays(2)),
            EventOccurrence(event, today.minusDays(1)),
            EventOccurrence(event, today.plusDays(3))
        )

        assertEquals(4, agendaTodayHeaderIndex(occurrences, today))
        assertEquals(2, agendaTodayHeaderIndex(occurrences.take(2), today))
        assertNull(agendaTodayHeaderIndex(emptyList(), today))
    }
}
