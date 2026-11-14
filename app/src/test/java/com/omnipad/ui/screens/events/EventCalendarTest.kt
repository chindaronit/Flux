package com.omnipad.ui.screens.events

import com.omnipad.data.model.EventExceptionModel
import com.omnipad.data.model.EventInstanceModel
import com.omnipad.data.model.EventModel
import com.omnipad.data.model.EventOccurrence
import com.omnipad.data.model.RecurrenceRule
import com.omnipad.data.model.filterCompletedEventOccurrences
import com.omnipad.data.model.findEventOccurrences
import com.omnipad.data.model.durationUntilEndTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventCalendarTest {
    @Test
    fun endTimeProducesSameDayOrOvernightDuration() {
        assertEquals(90 * 60_000L, durationUntilEndTime(LocalTime.of(9, 0), LocalTime.of(10, 30)))
        assertEquals(2 * 3_600_000L, durationUntilEndTime(LocalTime.of(23, 30), LocalTime.of(1, 30)))
        assertEquals(24 * 3_600_000L, durationUntilEndTime(LocalTime.of(9, 0), LocalTime.of(9, 0)))
    }

    @Test
    fun monthAndYearSelectorsCalculateDistantOffsetsAndBoundYears() {
        val now = YearMonth.of(2026, 11)
        assertEquals(-129, calendarMonthOffset(2016, Month.FEBRUARY, now))
        assertEquals(121, calendarMonthOffset(2036, Month.DECEMBER, now))
        assertNull(calendarMonthOffset(999999999, Month.JANUARY, now))
        Month.entries.forEach { month ->
            val years = calendarYearsForMonth(now, month)
            assertTrue(calendarMonthOffset(years.first, month, now) != null)
            assertNull(calendarMonthOffset(years.first - 1, month, now))
            assertTrue(calendarMonthOffset(years.last, month, now) != null)
            assertNull(calendarMonthOffset(years.last + 1, month, now))
        }
    }
    @Test
    fun focusTargetsOnlySelectedOccurrenceAndItsMonth() {
        val date = LocalDate.of(2027, 1, 2)
        val event = EventModel(id = "recurring")
        val focus = CalendarFocusRequest(event.id, date, 1)

        assertEquals(YearMonth.of(2027, 1), focus.month)
        assertTrue(focus.matches(EventOccurrence(event, date)))
        assertFalse(focus.matches(EventOccurrence(event, date.plusDays(1))))
        assertFalse(focus.matches(EventOccurrence(EventModel(id = "other"), date)))
    }

    @Test
    fun calendarDatesCoverCompleteWeeksAcrossYearBoundary() {
        val dates = calendarDates(YearMonth.of(2027, 1))

        assertEquals(LocalDate.of(2026, 12, 28), dates.first())
        assertEquals(LocalDate.of(2027, 1, 31), dates.last())
        assertEquals(35, dates.size)
    }

    @Test
    fun visibleWeeksIncludeRecurringAndAdjacentEventsButNotExceptionsOrCompletedInstances() {
        val dates = calendarDates(YearMonth.of(2027, 1))
        val start = dates.first()
        val recurring = EventModel(
            id = "daily",
            startDateTime = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            recurrence = RecurrenceRule.Custom()
        )
        val exceptionDate = start.plusDays(1)
        val completedDate = start.plusDays(2)
        val occurrences = filterCompletedEventOccurrences(
            findEventOccurrences(
                listOf(recurring),
                listOf(EventExceptionModel(recurring.id, exceptionDate.toEpochDay(), "")),
                dates.first(),
                dates.last()
            ),
            listOf(EventInstanceModel(eventId = recurring.id, instanceDate = completedDate.toEpochDay())),
            showCompletedEvents = false
        )

        assertEquals(dates.filterNot { it == exceptionDate || it == completedDate }, occurrences.map { it.date })
    }

    @Test
    fun dayListsOnlyItsVisibleOccurrencesIncludingAdjacentMonthDates() {
        val dates = calendarDates(YearMonth.of(2027, 1))
        val date = dates.first()
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val recurring = EventModel(id = "daily", startDateTime = start, recurrence = RecurrenceRule.Custom())
        val oneOff = EventModel(id = "once", startDateTime = start)
        val excluded = EventModel(id = "excluded", startDateTime = start, recurrence = RecurrenceRule.Custom())
        val completed = EventModel(id = "completed", startDateTime = start, recurrence = RecurrenceRule.Custom())
        val events = listOf(recurring, oneOff, excluded, completed)
        val exceptions = listOf(EventExceptionModel(excluded.id, date.toEpochDay(), ""))
        val instances = listOf(EventInstanceModel(eventId = completed.id, instanceDate = date.toEpochDay()))

        val hidden = calendarOccurrencesByDate(events, exceptions, instances, false, dates)
        assertEquals(setOf("daily", "once"), hidden[date].orEmpty().map { it.event.id }.toSet())
        assertEquals(setOf("daily", "excluded", "completed"), hidden[date.plusDays(1)].orEmpty().map { it.event.id }.toSet())

        val shown = calendarOccurrencesByDate(events, exceptions, instances, true, dates)
        assertEquals(setOf("daily", "once", "completed"), shown[date].orEmpty().map { it.event.id }.toSet())
    }

    @Test
    fun timelinePositionsDurationsAndPlacesOverlapsInSeparateColumns() {
        val date = LocalDate.of(2027, 1, 2)
        val zone = ZoneId.systemDefault()
        fun event(id: String, hour: Int, minute: Int, duration: Long?) = EventModel(
            id = id,
            startDateTime = date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli(),
            durationMillis = duration
        )
        val events = listOf(
            event("long", 9, 15, 90 * 60_000L),
            event("overlap", 10, 0, 60 * 60_000L),
            event("default", 11, 0, null)
        )
        val entries = dayTimelineEntries(date, events, emptyList(), emptyList(), true).second.associateBy { it.occurrence.event.id }
        assertEquals(9 * 3_600_000L + 15 * 60_000L, entries.getValue("long").start)
        assertEquals(10 * 3_600_000L + 45 * 60_000L, entries.getValue("long").end)
        assertEquals(2, entries.getValue("long").columns)
        assertTrue(entries.getValue("long").column != entries.getValue("overlap").column)
        assertEquals(30 * 60_000L, entries.getValue("default").end - entries.getValue("default").start)
        assertEquals(1, entries.getValue("default").columns)
    }

    @Test
    fun timelineIncludesOvernightOccurrencesAndAppliesTheirOwnCompletionAndExceptions() {
        val date = LocalDate.of(2027, 1, 2)
        val zone = ZoneId.systemDefault()
        val start = date.minusDays(1).atTime(23, 30).atZone(zone).toInstant().toEpochMilli()
        val event = EventModel(id = "night", startDateTime = start, durationMillis = 2 * 3_600_000L, recurrence = RecurrenceRule.Custom())
        val allDay = EventModel(id = "all-day", startDateTime = date.atStartOfDay(zone).toInstant().toEpochMilli(), isAllDay = true)
        val result = dayTimelineEntries(date, listOf(event, allDay), emptyList(), emptyList(), true)
        assertEquals(listOf("all-day"), result.first.map { it.event.id })
        val overnight = result.second.first { it.occurrence.date == date.minusDays(1) }
        assertEquals(0L, overnight.start)
        assertEquals(90 * 60_000L, overnight.end)
        val completed = listOf(EventInstanceModel(eventId = event.id, instanceDate = date.minusDays(1).toEpochDay()))
        val filtered = dayTimelineEntries(date, listOf(event), emptyList(), completed, false).second
        assertFalse(filtered.any { it.occurrence.date == date.minusDays(1) })
        val exception = listOf(EventExceptionModel(event.id, date.minusDays(1).toEpochDay(), ""))
        assertFalse(dayTimelineEntries(date, listOf(event), exception, emptyList(), true).second.any { it.occurrence.date == date.minusDays(1) })
    }
}
