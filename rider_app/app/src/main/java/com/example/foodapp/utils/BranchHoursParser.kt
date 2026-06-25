package com.example.foodapp.utils

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Parses the human-readable [operatingHours] string stored in Firestore and checks
 * whether the branch is currently open based on the device local time.
 *
 * Supported formats (case-insensitive):
 *  - "Open 24 hours"                       -> always open
 *  - "Closed"                              -> always closed
 *  - "9:00 AM - 10:00 PM"                 -> simple daily range (12-hour)
 *  - "09:00 - 22:00"                       -> simple daily range (24-hour)
 *  - "9am - 10pm"                          -> shorthand 12-hour range
 *  - "Mon-Fri 9am-10pm, Sat-Sun 10am-11pm" -> day-specific ranges
 *
 * Returns null when the string cannot be parsed; treat as open to avoid blocking
 * users when the data format is unknown.
 */
object BranchHoursParser {

    private val TIME_12H_FORMATTER   = DateTimeFormatter.ofPattern("h:mm a",  Locale.ENGLISH)
    private val TIME_12H_NM_FORMATTER = DateTimeFormatter.ofPattern("ha",     Locale.ENGLISH)
    private val TIME_24H_FORMATTER   = DateTimeFormatter.ofPattern("H:mm",    Locale.ENGLISH)

    /**
     * @param operatingHours The raw string from Firestore
     * @param now            The current LocalTime (injectable for unit tests)
     * @param today          The current DayOfWeek  (injectable for unit tests)
     * @return true=open, false=closed, null=unparseable
     */
    fun isOpenNow(
        operatingHours: String,
        now: LocalTime   = LocalTime.now(),
        today: DayOfWeek = java.time.LocalDate.now().dayOfWeek
    ): Boolean? {
        val norm = operatingHours.trim()
            .replace('\u2013', '-')   // en-dash
            .replace('\u2014', '-')   // em-dash
            .lowercase(Locale.ENGLISH)

        // Fast paths
        if (norm.contains("24 hour") || norm.contains("always open")) return true
        if (norm.trim() == "closed") return false

        val segments = norm.split(",").map { it.trim() }

        for (seg in segments) {
            val ds = parseDaySegment(seg)
            if (ds != null) {
                if (isTodayInRange(today, ds.first, ds.second)) {
                    return parseTimeRange(ds.third)?.let { (o, c) -> isTimeInRange(now, o, c) }
                }
            } else if (segments.size == 1) {
                // No day prefix; treat as universal daily range
                return parseTimeRange(seg)?.let { (o, c) -> isTimeInRange(now, o, c) }
            }
        }
        return false   // today not listed => branch closed today
    }

    // -- helpers ----------------------------------------------------------

    private val DAY_MAP = mapOf(
        "mon" to DayOfWeek.MONDAY,   "tue" to DayOfWeek.TUESDAY,
        "wed" to DayOfWeek.WEDNESDAY,"thu" to DayOfWeek.THURSDAY,
        "fri" to DayOfWeek.FRIDAY,   "sat" to DayOfWeek.SATURDAY,
        "sun" to DayOfWeek.SUNDAY
    )

    /** Returns Triple(fromDay, toDay, timeString) or null if no day prefix found. */
    private fun parseDaySegment(seg: String): Triple<DayOfWeek, DayOfWeek, String>? {
        val rangeRe = Regex("""^(mon|tue|wed|thu|fri|sat|sun)-(mon|tue|wed|thu|fri|sat|sun)\s+(.+)""")
        val singleRe = Regex("""^(mon|tue|wed|thu|fri|sat|sun)\s+(.+)""")

        rangeRe.find(seg)?.let { m ->
            val f = DAY_MAP[m.groupValues[1]] ?: return null
            val t = DAY_MAP[m.groupValues[2]] ?: return null
            return Triple(f, t, m.groupValues[3].trim())
        }
        singleRe.find(seg)?.let { m ->
            val d = DAY_MAP[m.groupValues[1]] ?: return null
            return Triple(d, d, m.groupValues[2].trim())
        }
        return null
    }

    private fun isTodayInRange(today: DayOfWeek, from: DayOfWeek, to: DayOfWeek): Boolean {
        val t = today.value; val f = from.value; val e = to.value
        return if (f <= e) t in f..e else t >= f || t <= e
    }

    /** Splits "9am-10pm" or "09:00 - 22:00" into an open/close LocalTime pair. */
    private fun parseTimeRange(s: String): Pair<LocalTime, LocalTime>? {
        // Split on hyphen NOT inside HH:MM (negative lookbehind on digit)
        val parts = s.split(Regex("""\s*-\s*(?=\d|[a-z])"""))
        if (parts.size != 2) return null
        val open  = parseTime(parts[0].trim()) ?: return null
        val close = parseTime(parts[1].trim()) ?: return null
        return open to close
    }

    private fun parseTime(raw: String): LocalTime? {
        // Normalise: "9am" -> "9 AM", "10pm" -> "10 PM", "9:30am" -> "9:30 AM"
        val up = raw.uppercase(Locale.ENGLISH)
            .replace(Regex("""(\d)(AM|PM)"""), "$1 $2")

        tryParse(up, TIME_12H_FORMATTER)?.let { return it }
        tryParse(up, TIME_12H_NM_FORMATTER)?.let { return it }
        tryParse(raw.trim(), TIME_24H_FORMATTER)?.let { return it }
        return null
    }

    private fun tryParse(s: String, fmt: DateTimeFormatter): LocalTime? = try {
        LocalTime.parse(s, fmt)
    } catch (_: DateTimeParseException) { null }

    private fun isTimeInRange(now: LocalTime, open: LocalTime, close: LocalTime): Boolean =
        if (open <= close) now >= open && now <= close
        else now >= open || now <= close   // overnight range e.g. 10pm-2am
}
