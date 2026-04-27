package com.example.capstone2026

import android.content.Context
import android.util.EventLogTags
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.DtEnd
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Summary
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.Categories
import java.io.InputStream
import java.util.Date
import java.util.Locale

fun loadIcsFromAssets(
    context: Context,
    fileName: String
): InputStream = context.assets.open(fileName)

fun parseIcsFile(input: InputStream): List<CalendarEvent> {
    val builder = CalendarBuilder()
    val calendar: Calendar = builder.build(input)

    val events = mutableListOf<CalendarEvent>()

    val vevents: List<VEvent> = calendar.getComponents(Component.VEVENT)

    for (vEvent in vevents) {

        val summaryProp = vEvent.getProperty(Summary.SUMMARY) as? Summary
        val dtStartProp = vEvent.getProperty(DtStart.DTSTART) as? DtStart
        val dtEndProp = vEvent.getProperty(DtEnd.DTEND) as? DtEnd
        val description = vEvent.getProperty(Description.DESCRIPTION) as? Description
        val category = vEvent.getProperty(Categories.CATEGORIES) as? Categories

        val title = summaryProp?.value ?: "Untitled"
        val eventType = category?.value ?: ""
        val notes = description?.value ?: ""

        val startDate = dtStartProp?.date as? Date
        val endDate = dtEndProp?.date as? Date

        if (startDate != null) {
            events.add(
                CalendarEvent(
                    title = title,
                    start = startDate,
                    end = endDate,
                    eventType = eventType,
                    notes = notes
                )
            )
        }
    }

    return events
}
