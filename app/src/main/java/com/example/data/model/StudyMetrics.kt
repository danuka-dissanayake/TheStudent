package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Small, allocation-light helpers shared by the dashboard and the drawer.
 *
 * `java.time` is not available on this app's minimum SDK (24, no desugaring), so
 * everything here goes through [Calendar] and [SimpleDateFormat] — the one date
 * API guaranteed on every device the app installs on.
 */
object StudyMetrics {

    /** Seven counts, Monday first, from the ISO-ish dates the server returns. */
    fun attemptsByWeekday(dates: List<String>): IntArray {
        val counts = IntArray(7)
        if (dates.isEmpty()) return counts
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        dates.forEach { raw ->
            val datePart = raw.substringBefore(' ').trim()
            val parsed = runCatching { parser.parse(datePart) }.getOrNull() ?: return@forEach
            calendar.time = parsed
            // Calendar: Sunday = 1 … Saturday = 7 → Monday-first index.
            val index = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
            counts[index] = counts[index] + 1
        }
        return counts
    }

    /** "2026-10-27 14:02:11" → "27 Oct". Empty when the value is not a date. */
    fun shortDate(raw: String): String {
        val parts = raw.substringBefore(' ').trim().split('-')
        if (parts.size != 3) return raw.substringBefore(' ').trim()
        val months = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        val month = parts[1].toIntOrNull()?.let { months.getOrNull(it - 1) } ?: return ""
        val day = parts[2].toIntOrNull() ?: return ""
        return "$day $month"
    }

    /** "Tuesday, 28 Oct" for today. */
    fun today(): String {
        val calendar = Calendar.getInstance()
        val day = listOf(
            "Sunday", "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday"
        )[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        val month = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )[calendar.get(Calendar.MONTH)]
        return "$day, ${calendar.get(Calendar.DAY_OF_MONTH)} $month"
    }

    /** "2h 15m" / "45m" / "0m". */
    fun duration(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "0m"
        }
    }
}
