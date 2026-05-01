package com.example.capstone2026

import com.example.capstone2026.network.EventDto
import com.example.capstone2026.ui.screens.toCalendarEvent
import org.junit.Assert.*
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

    @Test
    fun toCalendarEvent_handlesNullDescription() {
        val dto = EventDto(
            date = "2026-04-15",
            type = "quiz",
            description = null,
            assignment = null
        )

        val event = dto.toCalendarEvent("CSCE 310")

        assertNotNull(event)
        assertEquals("Quiz: ", event?.title)
    }

    @Test
    fun toCalendarEvent_handlesAssignmentAsNotes() {
        val dto = EventDto(
            date = "2026-04-15",
            type = "assignment",
            description = "Homework 1",
            assignment = "Chapter 3 problems"
        )

        val event = dto.toCalendarEvent("CSCE 310")

        assertEquals("Chapter 3 problems", event?.notes)
    }

    @Test
    fun toCalendarEvent_returnsNullForInvalidDate() {
        val dto = EventDto(
            date = "invalid-date",
            type = "exam",
            description = "Midterm",
            assignment = null
        )

        val event = dto.toCalendarEvent("CSCE 310")

        assertNull(event)
    }

    @Test
    fun toCalendarEvent_handlesEmptyCourseTitle() {
        val dto = EventDto(
            date = "2026-04-15",
            type = "exam",
            description = "Final Exam",
            assignment = null
        )

        val event = dto.toCalendarEvent(null)

        assertNotNull(event)
        assertNull(event?.courseTitle)
    }

    @Test
    fun toCalendarEvent_handlesDifferentEventTypes() {
        val dto = EventDto(
            date = "2026-04-15",
            type = "quiz",
            description = "Quiz 1",
            assignment = null
        )

        val event = dto.toCalendarEvent("CSCE 310")

        assertEquals("quiz", event?.eventType)
    }
}