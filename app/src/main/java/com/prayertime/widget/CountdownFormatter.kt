package com.prayertime.widget

object CountdownFormatter {
    fun format(
        millis: Long,
        hoursUnit: String,
        minutesUnit: String,
    ): String = formatInternal(millis.coerceAtLeast(0L), hoursUnit, minutesUnit, compact = false)

    /** No spaces — fits narrow 2×1 widget (e.g. {@code 2س15د}). */
    fun formatCompact(
        millis: Long,
        hoursUnit: String,
        minutesUnit: String,
    ): String = formatInternal(millis.coerceAtLeast(0L), hoursUnit, minutesUnit, compact = true)

    private fun formatInternal(
        millis: Long,
        hoursUnit: String,
        minutesUnit: String,
        compact: Boolean,
    ): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return buildString(if (hours > 0) (if (compact) 6 else 8) else (if (compact) 4 else 6)) {
            if (hours > 0) {
                append(hours)
                append(hoursUnit)
                if (!compact) append(' ')
                append(minutes)
                append(minutesUnit)
            } else {
                append(minutes)
                append(minutesUnit)
            }
        }
    }
}
