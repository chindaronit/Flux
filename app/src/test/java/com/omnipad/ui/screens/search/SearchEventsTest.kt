package com.omnipad.ui.screens.search

import com.omnipad.data.model.EventModel
import com.omnipad.data.model.EventExceptionModel
import com.omnipad.data.model.EventInstanceModel
import com.omnipad.data.model.RecurrenceRule
import com.omnipad.data.model.WorkspaceModel
import com.omnipad.data.model.filterCompletedEventOccurrences
import com.omnipad.data.model.findEventOccurrences
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchEventsTest {
    @Test
    fun searchIncludesAllCurrentUnlockedWorkspaces() {
        val workspaces = listOf(
            WorkspaceModel(workspaceId = "one"),
            WorkspaceModel(workspaceId = "locked", passKeyHash = "hash"),
            WorkspaceModel(workspaceId = "two")
        )
        assertEquals(listOf("one", "two"), searchableWorkspaceIds(workspaces))
        assertEquals(listOf("one", "two", "new"), searchableWorkspaceIds(workspaces + WorkspaceModel(workspaceId = "new")))
        assertEquals(listOf("one"), searchableWorkspaceIds(workspaces.take(2)))
    }

    @Test
    fun workspaceSearchIncludesOnlyTheEnteredWorkspaceEvenWhenLocked() {
        val workspaces = listOf(
            WorkspaceModel(workspaceId = "one"),
            WorkspaceModel(workspaceId = "locked", passKeyHash = "hash"),
            WorkspaceModel(workspaceId = "two")
        )
        assertEquals(listOf("one"), searchableWorkspaceIds(workspaces, "one"))
        assertEquals(listOf("locked"), searchableWorkspaceIds(workspaces, "locked"))
        assertEquals(emptyList<String>(), searchableWorkspaceIds(workspaces, "missing"))

        val events = listOf(
            EventModel(id = "one", workspaceId = "one", title = "Planning"),
            EventModel(id = "locked", workspaceId = "locked", title = "Planning"),
            EventModel(id = "two", workspaceId = "two", title = "Planning")
        )
        assertEquals(
            listOf("locked"),
            matchingSearchEvents(
                events,
                searchableWorkspaceIds(workspaces, "locked"),
                workspaces.filter { it.isLocked && it.workspaceId != "locked" }.map { it.workspaceId },
                "plan"
            ).map { it.id }
        )
    }

    @Test
    fun selectedTypesDetermineResultScope() {
        assertEquals(0, searchResultType(emptyList()))
        assertEquals(3, searchResultType(listOf(3)))
        assertEquals(1, searchResultType(listOf(1)))
        assertEquals(0, searchResultType(listOf(1, 3)))
    }

    @Test
    fun sectionIndicesAccountForDateHeadersPastHabitsAndProgressGroups() {
        assertEquals(
            mapOf(1 to 0, 2 to 3, 3 to 5, 4 to 11, 5 to 13, 7 to 18),
            searchSectionStartIndices(
                selectedTypes = listOf(1, 2, 3, 4, 5, 7),
                noteCount = 2,
                todoCount = 1,
                eventGroupSizes = listOf(2, 1),
                journalCount = 1,
                currentHabitCount = 1,
                pastHabitCount = 2,
                progressGroupSizes = listOf(3, 0, 2)
            )
        )
    }

    @Test
    fun sectionIndicesReflectFilteringAndSelectedTypeOrder() {
        val selected = listOf(7, 3, 1, 5)
        assertEquals(
            mapOf(3 to 0, 5 to 3),
            searchSectionStartIndices(selected, 0, 0, listOf(1), 0, 2, 0, listOf(0, 0, 0))
        )
        assertEquals(
            mapOf(7 to 0, 1 to 2),
            searchSectionStartIndices(selected, 1, 0, emptyList(), 0, 0, 0, listOf(0, 2, 0))
        )
        assertEquals(
            emptyMap<Int, Int>(),
            searchSectionStartIndices(selected, 0, 0, emptyList(), 0, 0, 0, emptyList())
        )
    }

    @Test
    fun filteredCalendarOccurrencesRespectRecurrenceExceptionsAndCompletion() {
        val date = LocalDate.of(2026, 11, 14)
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val events = listOf(
            EventModel(id = "a", workspaceId = "one", title = "Meeting", startDateTime = start, recurrence = RecurrenceRule.Custom()),
            EventModel(id = "b", workspaceId = "two", title = "Meeting", startDateTime = start),
            EventModel(id = "c", workspaceId = "two", title = "Other", startDateTime = start)
        )
        val matching = matchingSearchEvents(events, listOf("one", "two"), emptyList(), "meeting")
        val occurrences = findEventOccurrences(
            matching, listOf(EventExceptionModel("a", date.plusDays(1).toEpochDay(), "one")),
            date, date.plusDays(1)
        )
        val visible = filterCompletedEventOccurrences(
            occurrences,
            listOf(EventInstanceModel(eventId = "b", workspaceId = "two", instanceDate = date.toEpochDay())),
            showCompletedEvents = false
        )

        assertEquals(listOf("a"), visible.map { it.event.id })
        assertEquals(listOf(date), visible.map { it.date })
    }

    @Test
    fun searchMatchesTitlesAcrossSelectedUnlockedWorkspaces() {
        val events = listOf(
            EventModel(id = "one", workspaceId = "a", title = "Planning"),
            EventModel(id = "two", workspaceId = "b", title = "PLANNING session"),
            EventModel(id = "three", workspaceId = "c", title = "Planning"),
            EventModel(id = "four", workspaceId = "d", title = "Planning"),
            EventModel(id = "five", workspaceId = "a", title = "Review")
        )
        val selected = listOf("a", "b", "c")
        val locked = listOf("c")

        assertEquals(
            listOf("one", "two"),
            matchingSearchEvents(events, selected, locked, "plan").map { it.id }
        )
        assertEquals(
            listOf("one", "two", "five"),
            matchingSearchEvents(events, selected, locked, "").map { it.id }
        )
        assertEquals(
            listOf("one", "two", "five"),
            matchingSearchEvents(events, selected, locked, "  ").map { it.id }
        )
    }
}
