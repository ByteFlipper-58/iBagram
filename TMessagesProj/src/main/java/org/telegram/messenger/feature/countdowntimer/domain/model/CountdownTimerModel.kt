package org.telegram.messenger.feature.countdowntimer.domain.model

/**
 * Status of an individual countdown timer.
 */
enum class CountdownTimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

/**
 * Decomposed time components (days, hours, minutes, seconds).
 */
data class CountdownTimeComponents(
    val days: Long = 0,
    val hours: Long = 0,
    val minutes: Long = 0,
    val seconds: Long = 0,
    val totalSeconds: Long = 0
) {
    val isZero: Boolean get() = totalSeconds <= 0
}

/**
 * Snapshot of a timer tick event.
 */
data class CountdownTimerTick(
    val timerId: String,
    val remainingSeconds: Long,
    val initialSeconds: Long,
    val status: CountdownTimerStatus = CountdownTimerStatus.RUNNING,
    val progress: Float = 0f,
    val components: CountdownTimeComponents = CountdownTimeComponents(totalSeconds = remainingSeconds)
) {
    val isFinished: Boolean get() = remainingSeconds <= 0 || status == CountdownTimerStatus.FINISHED
    val isRunning: Boolean get() = status == CountdownTimerStatus.RUNNING
}

/**
 * Aggregated state of all active countdown timers.
 */
data class CountdownTimerState(
    val activeTimers: Map<String, CountdownTimerTick> = emptyMap()
)
