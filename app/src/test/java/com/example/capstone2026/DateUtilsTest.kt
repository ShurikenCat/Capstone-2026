package com.example.capstone2026

import com.example.capstone2026.util.cleanedEventTitle
import com.example.capstone2026.util.formatFullLocalDate
import com.example.capstone2026.util.formatMonthYear
import com.example.capstone2026.util.formatWeekRange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DateUtilsTest {

    @Test
    fun cleanedEventTitle_removesTimestamp() {
        val result = cleanedEventTitle("Midterm Exam 20260415T120000Z")

        assertEquals("Midterm Exam", result)
    }

    @Test
    fun cleanedEventTitle_removesExtraSpaces() {
        val result = cleanedEventTitle("  Final   Exam   ")

        assertEquals("Final Exam", result)
    }

    @Test
    fun cleanedEventTitle_keepsNormalTitleUnchanged() {
        val result = cleanedEventTitle("Homework 1 Due")

        assertEquals("Homework 1 Due", result)
    }

    @Test
    fun formatMonthYear_returnsReadableMonthAndYear() {
        val result = formatMonthYear(YearMonth.of(2026, 4))

        assertEquals("April 2026", result)
    }

    @Test
    fun formatFullLocalDate_returnsReadableDate() {
        val result = formatFullLocalDate(LocalDate.of(2026, 4, 15))

        assertEquals("Wednesday, April 15, 2026", result)
    }

    @Test
    fun formatWeekRange_returnsSundayToSaturdayRange() {
        val result = formatWeekRange(LocalDate.of(2026, 4, 12))

        assertEquals("Apr 12 - Apr 18, 2026", result)
    }
}