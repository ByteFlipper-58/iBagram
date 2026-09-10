package org.telegram.messenger.feature.countdowntimer.presentation

import org.telegram.messenger.feature.countdowntimer.domain.model.CountdownTimeComponents
import org.telegram.messenger.feature.countdowntimer.domain.model.CountdownTimerStatus

/**
 * UI State representing the active countdown timer and formatted presentation values.
 */
data class CountdownTimerUiState(
    val currentTimerId: String? = null,
    val remainingSeconds: Long = 0,
    val initialSeconds: Long = 0,
    val formattedTime: String = "00:00",
    val progress: Float = 0f,
    val status: CountdownTimerStatus = CountdownTimerStatus.IDLE,
    val components: CountdownTimeComponents = CountdownTimeComponents(),
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val errorMessage: String? = null
)
