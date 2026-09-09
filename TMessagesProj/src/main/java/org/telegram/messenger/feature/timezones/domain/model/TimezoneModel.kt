package org.telegram.messenger.feature.timezones.domain.model

import kotlin.math.abs

data class TimezoneModel(
    val id: String,
    val name: String,
    val utcOffsetSeconds: Int = 0
) {
    val formattedOffset: String
        get() {
            if (utcOffsetSeconds == 0) return "GMT"
            val sign = if (utcOffsetSeconds < 0) "-" else "+"
            val totalMinutes = abs(utcOffsetSeconds) / 60
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            val hoursStr = if (hours < 10) "0$hours" else hours.toString()
            val minutesStr = if (minutes < 10) "0$minutes" else minutes.toString()
            return "GMT$sign$hoursStr:$minutesStr"
        }

    val displayName: String
        get() = "$name, $formattedOffset"
}
