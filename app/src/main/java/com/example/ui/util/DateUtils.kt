package com.example.ui.util

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    // Initial business date requested in brief
    const val INITIAL_BUSINESS_DATE = "2026-10-24"

    private val isoFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val displayFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)
    private val monthYearFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

    /**
     * Returns today's actual calendar date in local timezone as YYYY-MM-DD
     */
    fun getActualTodayIso(): String {
        return LocalDate.now().format(isoFormat)
    }

    fun formatIsoToDisplay(isoDate: String): String {
        return try {
            val parsed = LocalDate.parse(isoDate, isoFormat)
            parsed.format(displayFormat)
        } catch (_: Exception) {
            isoDate
        }
    }

    fun formatIsoToMonthYear(isoDate: String): String {
        return try {
            val parsed = LocalDate.parse(isoDate, isoFormat)
            parsed.format(monthYearFormat)
        } catch (_: Exception) {
            isoDate
        }
    }

    fun parseLocalDate(isoDate: String): LocalDate {
        return try {
            LocalDate.parse(isoDate, isoFormat)
        } catch (_: Exception) {
            LocalDate.of(2026, 10, 24)
        }
    }

    fun toIsoString(date: LocalDate): String {
        return date.format(isoFormat)
    }
}
