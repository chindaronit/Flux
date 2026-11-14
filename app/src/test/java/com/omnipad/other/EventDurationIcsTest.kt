package com.omnipad.other

import com.omnipad.data.model.EventModel
import com.omnipad.data.model.RecurrenceRule
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class EventDurationIcsTest {
    @Test
    fun recurringOvernightDurationRoundTripsSeparatelyFromSeriesEnd() {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.of(2027, 1, 2).atTime(23, 30).atZone(zone).toInstant().toEpochMilli()
        val cutoff = LocalDate.of(2027, 2, 1).atTime(23, 30).atZone(zone).toInstant().toEpochMilli()
        val event = EventModel(
            id = "night",
            startDateTime = start,
            endDateTime = cutoff,
            durationMillis = 2 * 3_600_000L,
            recurrence = RecurrenceRule.Custom()
        )
        val imported = IcsImporter.import(IcsExporter.export(listOf(event))).single()
        assertEquals(event.durationMillis, imported.durationMillis)
        assertEquals(event.endDateTime, imported.endDateTime)
    }

    @Test
    fun legacyOneOffEndIsRetainedOnExport() {
        val start = LocalDate.of(2027, 1, 2).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val event = EventModel(startDateTime = start, endDateTime = start + 3_600_000L)
        val imported = IcsImporter.import(IcsExporter.export(listOf(event))).single()
        assertEquals(3_600_000L, imported.durationMillis)
    }
}
