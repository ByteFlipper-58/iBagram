package org.telegram.messenger.feature.system.countdowntimer.data.mapper

import org.telegram.messenger.feature.system.countdowntimer.domain.model.CountdownTimeComponents
import org.telegram.messenger.feature.system.countdowntimer.domain.model.CountdownTimerStatus
import org.telegram.messenger.feature.system.countdowntimer.domain.model.CountdownTimerTick

/**
 * Pure mathematical mapper for countdown timer calculations and formatting.
 */
object CountdownTimerMapper {

    fun calculateProgress(remainingSeconds: Long, initialSeconds: Long): Float {
        if (initialSeconds <= 0) return 1f
        val elapsed = (initialSeconds - remainingSeconds).coerceIn(0, initialSeconds)
        return elapsed.toFloat() / initialSeconds.toFloat()
    }

    fun toComponents(totalSeconds: Long): CountdownTimeComponents {
        val safeSeconds = totalSeconds.coerceAtLeast(0)
        val days = safeSeconds / 86400
        val remainingAfterDays = safeSeconds % 86400
        val hours = remainingAfterDays / 3600
        val remainingAfterHours = remainingAfterDays % 3600
        val minutes = remainingAfterHours / 60
        val seconds = remainingAfterHours % 60

        return CountdownTimeComponents(
            days = days,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            totalSeconds = safeSeconds
        )
    }

    fun buildTick(
        timerId: String,
        remainingSeconds: Long,
        initialSeconds: Long,
        status: CountdownTimerStatus = CountdownTimerStatus.RUNNING
    ): CountdownTimerTick {
        val safeRemaining = remainingSeconds.coerceAtLeast(0)
        val finalStatus = if (safeRemaining <= 0) CountdownTimerStatus.FINISHED else status
        return CountdownTimerTick(
            timerId = timerId,
            remainingSeconds = safeRemaining,
            initialSeconds = initialSeconds.coerceAtLeast(0),
            status = finalStatus,
            progress = calculateProgress(safeRemaining, initialSeconds),
            components = toComponents(safeRemaining)
        )
    }

    fun format(totalSeconds: Long): String {
        val components = toComponents(totalSeconds)
        return when {
            components.days > 0 -> {
                String.format("%dd %02d:%02d:%02d", components.days, components.hours, components.minutes, components.seconds)
            }
            components.hours > 0 -> {
                String.format("%02d:%02d:%02d", components.hours, components.minutes, components.seconds)
            }
            else -> {
                String.format("%02d:%02d", components.minutes, components.seconds)
            }
        }
    }
}
