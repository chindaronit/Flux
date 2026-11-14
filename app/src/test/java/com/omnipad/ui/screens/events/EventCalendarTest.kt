package com.omnipad.ui.screens.events

import com.omnipad.data.model.EventExceptionModel
import com.omnipad.data.model.EventInstanceModel
import com.omnipad.data.model.EventModel
import com.omnipad.data.model.EventOccurrence
import com.omnipad.data.model.RecurrenceRule
import com.omnipad.data.model.filterCompletedEventOccurrences
import com.omnipad.data.model.findEventOccurrences
import java.time.LocalDate
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
}
