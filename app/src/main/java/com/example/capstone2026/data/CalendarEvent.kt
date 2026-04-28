package com.example.capstone2026.data

import java.util.Date

data class CalendarEvent(
    val title: String,
    val start: Date,
    val end: Date? = null,
    val eventType: String? = null,
    val notes: String? = null,
    val isAllDay: Boolean = false,
    val courseTitle: String? = null
)