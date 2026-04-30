package com.example.capstone2026

import com.example.capstone2026.network.EventDto
import com.example.capstone2026.ui.screens.toCalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EventDtoTest {

    @Test
    fun toCalendarEvent_convertsDtoIntoCalendarEvent() {
        val dto = EventDto(
            date = "2026-04-15",
            type = "exam",
            description = "Midterm Exam",
            assignment = null
        )

        val event = dto.toCalendarEvent("CSCE 310")

        assertNotNull(event)
        assertEquals("Exam: Midterm Exam", event?.title)
        assertEquals("exam", event?.eventType)
        assertEquals("CSCE 310", event?.courseTitle)
    }
}