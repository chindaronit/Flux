package com.flux.other

import com.flux.data.model.EventModel
import com.flux.data.model.RecurrenceRule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/* ---------- Formatting helpers ---------- */

private val ICS_DATETIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

private fun Long.toIcsDateTime(): String = ICS_DATETIME_FORMAT.format(Instant.ofEpochMilli(this))

private fun String.icsDateTimeToMillis(): Long {
    // Accepts "20260823T093000Z" (UTC datetime) or "20260823" (all-day date, VALUE=DATE)
    val clean = trim().removeSuffix("Z")
    return if (clean.contains("T")) {
        val ldt = java.time.LocalDateTime.parse(clean, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
        ldt.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
    } else {
        // Date-only value (all-day event) — anchor to UTC midnight of that date.
        val date = LocalDate.parse(clean, DateTimeFormatter.ofPattern("yyyyMMdd"))
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}

private fun String.escapeIcsText(): String =
    replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")

private fun String.unescapeIcsText(): String =
    replace("\\n", "\n").replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")

/* ---------- RecurrenceRule <-> RRULE ---------- */

// index 0=Mon .. 6=Sun, matching EventModel.occursOn()/isActiveOn()'s day indexing
private val WEEKDAY_CODES = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")

/**
 * Builds the value that goes after "RRULE:" in the ICS file, or null if this event
 * should be emitted as a single non-recurring VEVENT.
 *
 * NOTE: endDateTime is only meaningful here as the recurrence cutoff (UNTIL) — it is
 * NOT a per-occurrence duration, because EventModel has no field for that.
 */
fun RecurrenceRule.toIcsRRule(endDateTime: Long): String? {
    val core = when (this) {
        RecurrenceRule.NONE, RecurrenceRule.Once -> return null
        is RecurrenceRule.Weekly -> {
            val days = daysOfWeek.sorted().joinToString(",") { WEEKDAY_CODES.getOrElse(it) { "MO" } }
            "FREQ=WEEKLY;BYDAY=$days"
        }
        RecurrenceRule.Monthly -> "FREQ=MONTHLY"
        RecurrenceRule.Yearly -> "FREQ=YEARLY"
        is RecurrenceRule.Custom -> {
            if (everyXDays <= 1) "FREQ=DAILY" else "FREQ=DAILY;INTERVAL=$everyXDays"
        }
    }
    return if (endDateTime > 0) "$core;UNTIL=${endDateTime.toIcsDateTime()}" else core
}

/**
 * Parses an RRULE line's value (everything after "RRULE:") into a RecurrenceRule plus
 * an optional UNTIL in epoch millis.
 *
 * Anything we don't natively support (BYSETPOS, MONTHLY+BYDAY, etc.) degrades to
 * Custom(1) rather than being dropped, so the import at least produces *something*
 * sane instead of silently losing the event.
 */
fun parseIcsRRule(rruleValue: String): Pair<RecurrenceRule, Long?> {
    val params = rruleValue.split(";")
        .mapNotNull {
            val idx = it.indexOf('=')
            if (idx == -1) null else it.substring(0, idx) to it.substring(idx + 1)
        }
        .toMap()

    val until = params["UNTIL"]?.let { runCatching { it.icsDateTimeToMillis() }.getOrNull() }

    val rule = when (params["FREQ"]) {
        "WEEKLY" -> {
            val days = params["BYDAY"]?.split(",")
                ?.mapNotNull { code -> WEEKDAY_CODES.indexOf(code).takeIf { it >= 0 } }
                ?: (0..6).toList()
            RecurrenceRule.Weekly(daysOfWeek = days)
        }
        "MONTHLY" -> RecurrenceRule.Monthly
        "YEARLY" -> RecurrenceRule.Yearly
        "DAILY" -> RecurrenceRule.Custom(everyXDays = params["INTERVAL"]?.toIntOrNull() ?: 1)
        else -> RecurrenceRule.Custom(1)
    }
    return rule to until
}

/* ---------- Export ---------- */

object IcsExporter {

    fun export(events: List<EventModel>): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//Flux//Android//EN")
        sb.appendLine("CALSCALE:GREGORIAN")

        events.forEach { event -> sb.append(exportEvent(event)) }

        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }

    private fun exportEvent(event: EventModel): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VEVENT")
        sb.appendLine("UID:${event.id}@flux")
        sb.appendLine("DTSTAMP:${System.currentTimeMillis().toIcsDateTime()}")
        sb.appendLine("DTSTART:${event.startDateTime.toIcsDateTime()}")

        val isRecurring = event.recurrence !is RecurrenceRule.Once && event.recurrence != RecurrenceRule.NONE
        if (!isRecurring) {
            // Only a genuinely one-off event has a real DTEND; for recurring events
            // endDateTime is expressed as RRULE;UNTIL instead (see toIcsRRule).
            val dtEnd = if (event.endDateTime > 0) event.endDateTime else event.startDateTime
            sb.appendLine("DTEND:${dtEnd.toIcsDateTime()}")
        }

        sb.appendLine("SUMMARY:${event.title.escapeIcsText()}")
        if (event.description.isNotBlank()) {
            sb.appendLine("DESCRIPTION:${event.description.escapeIcsText()}")
        }

        event.recurrence.toIcsRRule(event.endDateTime)?.let { rrule ->
            sb.appendLine("RRULE:$rrule")
        }

        if (event.notificationOffset > 0) {
            val minutes = event.notificationOffset / 60_000
            sb.appendLine("BEGIN:VALARM")
            sb.appendLine("ACTION:DISPLAY")
            sb.appendLine("DESCRIPTION:${event.title.escapeIcsText()}")
            sb.appendLine("TRIGGER:-PT${minutes}M")
            sb.appendLine("END:VALARM")
        }

        sb.appendLine("END:VEVENT")
        return sb.toString()
    }
}

/* ---------- Import ---------- */

object IcsImporter {

    /**
     * Naive but spec-correct-enough parser for the VEVENT subset Flux needs.
     * Doesn't attempt to reconstruct EventInstanceModel history — imported events
     * start with no completion instances.
     */
    fun import(icsContent: String, workspaceId: String = ""): List<EventModel> {
        val lines = unfoldLines(icsContent)
        val events = mutableListOf<EventModel>()

        var inEvent = false
        var inAlarm = false
        var uid: String? = null
        var summary = ""
        var description = ""
        var dtStart: Long? = null
        var dtEnd: Long? = null
        var rrule: String? = null

        fun flush() {
            val start = dtStart ?: return
            val (recurrence, until) = rrule?.let { parseIcsRRule(it) } ?: (RecurrenceRule.Once to null)
            val endDateTime = when {
                until != null -> until              // recurring: UNTIL -> our recurrence cutoff
                recurrence == RecurrenceRule.Once -> dtEnd ?: -1L
                else -> -1L                           // recurring, no UNTIL -> open-ended
            }
            events += EventModel(
                id = uid ?: UUID.randomUUID().toString(),
                title = summary,
                description = description,
                recurrence = recurrence,
                startDateTime = start,
                endDateTime = endDateTime,
                workspaceId = workspaceId
            )
        }

        for (raw in lines) {
            val line = raw.trim()
            when {
                line == "BEGIN:VEVENT" -> {
                    inEvent = true
                    inAlarm = false
                    uid = null; summary = ""; description = ""
                    dtStart = null; dtEnd = null; rrule = null
                }
                line == "END:VEVENT" -> {
                    if (inEvent) flush()
                    inEvent = false
                }
                // VALARM (and any other) sub-blocks have their own UID/DESCRIPTION/etc that
                // must NOT be mistaken for the parent VEVENT's properties.
                line == "BEGIN:VALARM" -> inAlarm = true
                line == "END:VALARM" -> inAlarm = false
                !inEvent || inAlarm -> Unit
                line.startsWith("UID") -> uid = valueOf(line)
                line.startsWith("SUMMARY") -> summary = valueOf(line).unescapeIcsText()
                line.startsWith("DESCRIPTION") -> description = valueOf(line).unescapeIcsText()
                line.startsWith("DTSTART") ->
                    dtStart = runCatching { valueOf(line).icsDateTimeToMillis() }.getOrNull()
                line.startsWith("DTEND") ->
                    dtEnd = runCatching { valueOf(line).icsDateTimeToMillis() }.getOrNull()
                line.startsWith("RRULE") -> rrule = valueOf(line)
            }
        }
        return events
    }

    /** Extracts the value part of a "NAME;PARAM=x:VALUE" or "NAME:VALUE" content line. */
    private fun valueOf(line: String): String {
        val colonIdx = line.indexOf(':')
        return if (colonIdx == -1) "" else line.substring(colonIdx + 1)
    }

    /** RFC5545 line unfolding: a line starting with space/tab continues the previous line. */
    private fun unfoldLines(content: String): List<String> {
        val rawLines = content.split("\r\n", "\n")
        val result = mutableListOf<String>()
        for (line in rawLines) {
            if ((line.startsWith(" ") || line.startsWith("\t")) && result.isNotEmpty()) {
                result[result.lastIndex] = result.last() + line.drop(1)
            } else {
                result.add(line)
            }
        }
        return result
    }
}