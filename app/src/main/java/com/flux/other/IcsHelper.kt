package com.flux.other

import com.flux.data.model.EventModel
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.effectiveReminderOffsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/* ---------- Formatting helpers ---------- */

private val ICS_DATETIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
private val ICS_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd")

private fun Long.toIcsDateTime(): String = ICS_DATETIME_FORMAT.format(Instant.ofEpochMilli(this))

private fun Long.toIcsDate(): String = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()
    .format(ICS_DATE_FORMAT)

private fun String.icsDateTimeToMillis(): Long {
    // Accepts "20260823T093000Z" (UTC datetime) or "20260823" (all-day date, VALUE=DATE)
    val clean = trim().removeSuffix("Z")
    return if (clean.contains("T")) {
        val ldt = java.time.LocalDateTime.parse(clean, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
        ldt.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
    } else {
        // Date-only values represent a local calendar date, not a UTC instant.
        val date = LocalDate.parse(clean, DateTimeFormatter.ofPattern("yyyyMMdd"))
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

private fun String.escapeIcsText(): String =
    replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")

private fun String.unescapeIcsText(): String =
    replace("\\n", "\n").replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")

private fun Long.toIcsTrigger(): String {
    var seconds = (this.coerceAtLeast(0L) / 1_000L)
    if (seconds == 0L) return "PT0S"

    val days = seconds / 86_400
    seconds %= 86_400
    val hours = seconds / 3_600
    seconds %= 3_600
    val minutes = seconds / 60
    seconds %= 60
    return buildString {
        append("-P")
        if (days > 0) append("${days}D")
        if (hours > 0 || minutes > 0 || seconds > 0) {
            append('T')
            if (hours > 0) append("${hours}H")
            if (minutes > 0) append("${minutes}M")
            if (seconds > 0) append("${seconds}S")
        }
    }
}

private fun parseIcsTriggerOffset(value: String): Long? {
    val match = Regex("""^(-)?P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?$""")
        .matchEntire(value.trim()) ?: return null
    // Positive triggers occur after the event and cannot be represented as a reminder offset.
    if (match.groupValues[1].isEmpty() && value != "PT0S") return null
    val days = match.groupValues[2].toLongOrNull() ?: 0L
    val hours = match.groupValues[3].toLongOrNull() ?: 0L
    val minutes = match.groupValues[4].toLongOrNull() ?: 0L
    val seconds = match.groupValues[5].toLongOrNull() ?: 0L
    return (((days * 24 + hours) * 60 + minutes) * 60 + seconds) * 1_000L
}

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
fun RecurrenceRule.toIcsRRule(endDateTime: Long, isAllDay: Boolean = false): String? {
    val core = when (this) {
        RecurrenceRule.NONE, RecurrenceRule.Once -> return null
        is RecurrenceRule.Weekly -> {
            val days = daysOfWeek.sorted().joinToString(",") { WEEKDAY_CODES.getOrElse(it) { "MO" } }
            "FREQ=WEEKLY;BYDAY=$days"
        }
        RecurrenceRule.Monthly -> "FREQ=MONTHLY"
        RecurrenceRule.Yearly -> "FREQ=YEARLY"
        is RecurrenceRule.Interval -> {
            val frequency = when (unit) {
                com.flux.data.model.IntervalUnit.DAY -> "DAILY"
                com.flux.data.model.IntervalUnit.WEEK -> "WEEKLY"
                com.flux.data.model.IntervalUnit.MONTH -> "MONTHLY"
                com.flux.data.model.IntervalUnit.YEAR -> "YEARLY"
            }
            val interval = if (every > 1) ";INTERVAL=$every" else ""
            val days = if (unit == com.flux.data.model.IntervalUnit.WEEK && daysOfWeek.isNotEmpty()) {
                ";BYDAY=" + daysOfWeek.sorted().joinToString(",") {
                    WEEKDAY_CODES.getOrElse(it) { "MO" }
                }
            } else {
                ""
            }
            "FREQ=$frequency$interval$days"
        }
        is RecurrenceRule.Custom -> {
            if (everyXDays <= 1) "FREQ=DAILY" else "FREQ=DAILY;INTERVAL=$everyXDays"
        }
    }
    val until = if (isAllDay) endDateTime.toIcsDate() else endDateTime.toIcsDateTime()
    return if (endDateTime > 0) "$core;UNTIL=$until" else core
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
            val interval = params["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            if (interval > 1) {
                RecurrenceRule.Interval(com.flux.data.model.IntervalUnit.WEEK, interval, days)
            } else {
                RecurrenceRule.Weekly(daysOfWeek = days)
            }
        }
        "MONTHLY" -> {
            val interval = params["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            if (interval > 1) RecurrenceRule.Interval(com.flux.data.model.IntervalUnit.MONTH, interval)
            else RecurrenceRule.Monthly
        }
        "YEARLY" -> {
            val interval = params["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            if (interval > 1) RecurrenceRule.Interval(com.flux.data.model.IntervalUnit.YEAR, interval)
            else RecurrenceRule.Yearly
        }
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
        if (event.isAllDay) {
            sb.appendLine("DTSTART;VALUE=DATE:${event.startDateTime.toIcsDate()}")
        } else {
            sb.appendLine("DTSTART:${event.startDateTime.toIcsDateTime()}")
        }

        val isRecurring = event.recurrence !is RecurrenceRule.Once && event.recurrence != RecurrenceRule.NONE
        if (!isRecurring && !event.isAllDay) {
            // Only a genuinely one-off event has a real DTEND; for recurring events
            // endDateTime is expressed as RRULE;UNTIL instead (see toIcsRRule).
            val dtEnd = if (event.endDateTime > 0) event.endDateTime else event.startDateTime
            sb.appendLine("DTEND:${dtEnd.toIcsDateTime()}")
        }

        sb.appendLine("SUMMARY:${event.title.escapeIcsText()}")
        if (event.description.isNotBlank()) {
            sb.appendLine("DESCRIPTION:${event.description.escapeIcsText()}")
        }

        event.recurrence.toIcsRRule(event.endDateTime, event.isAllDay)?.let { rrule ->
            sb.appendLine("RRULE:$rrule")
        }

        event.effectiveReminderOffsets().forEach { offset ->
            sb.appendLine("BEGIN:VALARM")
            sb.appendLine("ACTION:DISPLAY")
            sb.appendLine("DESCRIPTION:${event.title.escapeIcsText()}")
            sb.appendLine("TRIGGER:${offset.toIcsTrigger()}")
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
        var isAllDay = false
        var dtEnd: Long? = null
        var rrule: String? = null
        val notificationOffsets = mutableListOf<Long>()

        fun flush() {
            val start = dtStart ?: return
            val (recurrence, until) = rrule?.let { parseIcsRRule(it) } ?: (RecurrenceRule.Once to null)
            val endDateTime = when {
                until != null -> until              // recurring: UNTIL -> our recurrence cutoff
                isAllDay -> -1L
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
                notificationOffset = notificationOffsets.firstOrNull() ?: 0L,
                notificationOffsets = notificationOffsets.toList(),
                workspaceId = workspaceId,
                isAllDay = isAllDay
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
                    isAllDay = false
                    notificationOffsets.clear()
                }
                line == "END:VEVENT" -> {
                    if (inEvent) flush()
                    inEvent = false
                }
                // VALARM (and any other) sub-blocks have their own UID/DESCRIPTION/etc that
                // must NOT be mistaken for the parent VEVENT's properties.
                line == "BEGIN:VALARM" -> inAlarm = true
                line == "END:VALARM" -> inAlarm = false
                !inEvent -> Unit
                inAlarm && line.startsWith("TRIGGER") -> {
                    parseIcsTriggerOffset(valueOf(line))?.let(notificationOffsets::add)
                }
                inAlarm -> Unit
                line.startsWith("UID") -> uid = valueOf(line)
                line.startsWith("SUMMARY") -> summary = valueOf(line).unescapeIcsText()
                line.startsWith("DESCRIPTION") -> description = valueOf(line).unescapeIcsText()
                line.startsWith("DTSTART") -> {
                    isAllDay = line.substringBefore(':')
                        .contains("VALUE=DATE", ignoreCase = true)
                    dtStart = runCatching { valueOf(line).icsDateTimeToMillis() }.getOrNull()
                }
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