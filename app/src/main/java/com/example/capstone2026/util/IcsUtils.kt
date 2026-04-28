package com.example.capstone2026.util
import com.example.capstone2026.data.CalendarEvent
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Utility functions for reading and writing calendar events
 * to an ICS file stored locally on the device.
 */
fun ensureWritableIcs(context: android.content.Context): File {
    val dir = context.filesDir
    val target = File(dir, "schedule.ics")

    if (!target.exists()) {
        try {
            context.assets.open("schedule.ics").use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            target.writeText("BEGIN:VCALENDAR\nVERSION:2.0\nEND:VCALENDAR\n")
        }
    }
    return target
}

fun List<CalendarEvent>.toIcsString(): String {
    val sb = StringBuilder()
    sb.appendLine("BEGIN:VCALENDAR")
    sb.appendLine("VERSION:2.0")

    val fmt = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
    fmt.timeZone = TimeZone.getTimeZone("UTC")
    fun escapeIcs(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    }
    for ((index, event) in this.withIndex()) {
        sb.appendLine("BEGIN:VEVENT")
        sb.appendLine("UID:app-$index@capstone2026")
        sb.appendLine("SUMMARY:${escapeIcs(event.title)}")
        sb.appendLine("DTSTART:${fmt.format(event.start)}")
        event.end?.let {
            sb.appendLine("DTEND:${fmt.format(it)}")
        }
        event.notes?.let{
            sb.appendLine("DESCRIPTION:${escapeIcs(event.notes)}")
        }
        event.eventType?.let{
            sb.appendLine("CATEGORIES:${escapeIcs(event.eventType)}")
        }
        sb.appendLine("END:VEVENT")
    }

    sb.appendLine("END:VCALENDAR")
    return sb.toString()
}

fun saveEventsToIcs(context: android.content.Context, events: List<CalendarEvent>) {
    val file = ensureWritableIcs(context)
    file.writeText(events.toIcsString())
}