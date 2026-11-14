package com.omnipad.data.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurrenceRuleSerializationTest {
    private val rules = listOf(
        "NONE" to RecurrenceRule.NONE,
        "Once" to RecurrenceRule.Once,
        "Weekly" to RecurrenceRule.Weekly(listOf(1, 4)),
        "Monthly" to RecurrenceRule.Monthly,
        "Yearly" to RecurrenceRule.Yearly,
        "Custom" to RecurrenceRule.Custom(5),
        "Interval" to RecurrenceRule.Interval(IntervalUnit.WEEK, 2, listOf(0, 3))
    )

    @Test
    fun allRulesReadLegacyAndCurrentNamesAndWriteStableNames() {
        val converter = Converter()
        for ((name, rule) in rules) {
            val encoded = converter.fromRecurrence(rule)
            assertTrue(encoded.contains("\"type\":\"$name\""))
            assertEquals(rule, converter.toRecurrence(encoded))
            for (prefix in listOf(
                "com.flux.data.model.RecurrenceRule.",
                "com.omnipad.data.model.RecurrenceRule."
            )) {
                val oldJson = encoded.replace("\"type\":\"$name\"", "\"type\":\"$prefix$name\"")
                assertEquals(rule, converter.toRecurrence(oldJson))
                assertEquals(rule, Json.decodeFromString(RecurrenceRuleCompatSerializer, oldJson))
            }
        }
    }

    @Test
    fun unknownRecurrenceDoesNotSilentlyBecomeOnce() {
        assertThrows(SerializationException::class.java) {
            Converter().toRecurrence("""{"type":"com.flux.data.model.RecurrenceRule.Unknown"}""")
        }
        assertEquals(RecurrenceRule.Once, Converter().toRecurrence(null))
    }
}
