package com.example.capstone2026

import com.example.capstone2026.util.cleanedEventTitle
import com.example.capstone2026.util.formatMonthYear
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class DateUtilsTest {

    @Test
    fun cleanedEventTitle_removesTimestamp() {
        val result = cleanedEventTitle("Midterm Exam 20260415T120000Z")

        assertEquals("Midterm Exam", result)
    }

    @Test
    fun formatMonthYear_returnsReadableMonthAndYear() {
        val result = formatMonthYear(YearMonth.of(2026, 4))

        assertEquals("April 2026", result)
    }
}