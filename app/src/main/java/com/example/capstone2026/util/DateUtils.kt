package com.example.capstone2026.util

import com.example.capstone2026.data.CalendarEvent
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Helper functions for formatting dates and converting between types.
 */
fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    return formatter.format(date)
}

fun formatEventDate(event: CalendarEvent): String {
    return if (event.isAllDay) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(event.start) + " • All day"
    } else {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(event.start)
    }
}

fun formatFullLocalDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
}

fun formatWeekRange(startDate: LocalDate): String {
    val endDate = startDate.plusDays(6)
    return "${startDate.format(DateTimeFormatter.ofPattern("MMM d"))} - " +
            "${endDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
}

fun formatMonthYear(yearMonth: YearMonth): String {
    return yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
}

fun cleanedEventTitle(title: String): String {
    return title
        .replace(Regex("""\b\d{8}T\d{6}Z\b"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

fun Date.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(time)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

fun isSameDay(d1: Date, d2: Date): Boolean {
    val c1 = Calendar.getInstance().apply { time = d1 }
    val c2 = Calendar.getInstance().apply { time = d2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}