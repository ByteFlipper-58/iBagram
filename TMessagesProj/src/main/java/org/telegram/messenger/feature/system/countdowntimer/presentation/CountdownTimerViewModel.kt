package org.telegram.messenger.feature.system.countdowntimer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.system.countdowntimer.domain.model.CountdownTimeComponents
import org.telegram.messenger.feature.system.countdowntimer.domain.model.CountdownTimerStatus
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.ClearAllCountdownTimersUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.FormatCountdownTimeUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.GetCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.IsCountdownTimerRunningUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.ObserveCountdownStateUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.PauseCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.ResumeCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.StartCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.StopCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.TickCountdownTimerUseCase

class CountdownTimerViewModel(
    private val startCountdownTimerUseCase: StartCountdownTimerUseCase,
    private val stopCountdownTimerUseCase: StopCountdownTimerUseCase,
    private val pauseCountdownTimerUseCase: PauseCountdownTimerUseCase,
    private val resumeCountdownTimerUseCase: ResumeCountdownTimerUseCase,
    private val getCountdownTimerUseCase: GetCountdownTimerUseCase,
    private val isCountdownTimerRunningUseCase: IsCountdownTimerRunningUseCase,
    private val tickCountdownTimerUseCase: TickCountdownTimerUseCase,
    private val clearAllCountdownTimersUseCase: ClearAllCountdownTimersUseCase,
    private val observeCountdownStateUseCase: ObserveCountdownStateUseCase,
    private val formatCountdownTimeUseCase: FormatCountdownTimeUseCase = FormatCountdownTimeUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CountdownTimerUiState())
    val uiState: StateFlow<CountdownTimerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeCountdownStateUseCase().collectLatest { state ->
                val activeId = _uiState.value.currentTimerId
                val tick = if (activeId != null) state.activeTimers[activeId] else null
                if (tick != null) {
                    _uiState.update { current ->
                        current.copy(
                            remainingSeconds = tick.remainingSeconds,
                            initialSeconds = tick.initialSeconds,
                            formattedTime = formatCountdownTimeUseCase(tick.remainingSeconds),
                            progress = tick.progress,
                            status = tick.status,
                            components = tick.components,
                            isRunning = tick.isRunning,
                            isFinished = tick.isFinished
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: CountdownTimerEvent) {
        when (event) {
            is CountdownTimerEvent.Start -> {
                val tick = startCountdownTimerUseCase(event.timerId, event.seconds)
                _uiState.update {
                    it.copy(
                        currentTimerId = event.timerId,
                        remainingSeconds = tick.remainingSeconds,
                        initialSeconds = tick.initialSeconds,
                        formattedTime = formatCountdownTimeUseCase(tick.remainingSeconds),
                        progress = tick.progress,
                        status = tick.status,
                        components = tick.components,
                        isRunning = tick.isRunning,
                        isFinished = tick.isFinished
                    )
                }
            }
            is CountdownTimerEvent.Stop -> {
                val tick = stopCountdownTimerUseCase(event.timerId)
                if (tick != null && _uiState.value.currentTimerId == event.timerId) {
                    _uiState.update {
                        it.copy(
                            status = tick.status,
                            isRunning = tick.isRunning,
                            isFinished = tick.isFinished
                        )
                    }
                }
            }
            is CountdownTimerEvent.Pause -> {
                val tick = pauseCountdownTimerUseCase(event.timerId)
                if (tick != null && _uiState.value.currentTimerId == event.timerId) {
                    _uiState.update {
                        it.copy(
                            status = tick.status,
                            isRunning = tick.isRunning
                        )
                    }
                }
            }
            is CountdownTimerEvent.Resume -> {
                val tick = resumeCountdownTimerUseCase(event.timerId)
                if (tick != null && _uiState.value.currentTimerId == event.timerId) {
                    _uiState.update {
                        it.copy(
                            status = tick.status,
                            isRunning = tick.isRunning
                        )
                    }
                }
            }
            is CountdownTimerEvent.TickManual -> {
                val tick = tickCountdownTimerUseCase(event.timerId, event.stepSeconds)
                if (tick != null && _uiState.value.currentTimerId == event.timerId) {
                    _uiState.update {
                        it.copy(
                            remainingSeconds = tick.remainingSeconds,
                            formattedTime = formatCountdownTimeUseCase(tick.remainingSeconds),
                            progress = tick.progress,
                            status = tick.status,
                            components = tick.components,
                            isRunning = tick.isRunning,
                            isFinished = tick.isFinished
                        )
                    }
                }
            }
            is CountdownTimerEvent.SelectTimer -> {
                val tick = getCountdownTimerUseCase(event.timerId)
                if (tick != null) {
                    _uiState.update {
                        it.copy(
                            currentTimerId = event.timerId,
                            remainingSeconds = tick.remainingSeconds,
                            initialSeconds = tick.initialSeconds,
                            formattedTime = formatCountdownTimeUseCase(tick.remainingSeconds),
                            progress = tick.progress,
                            status = tick.status,
                            components = tick.components,
                            isRunning = tick.isRunning,
                            isFinished = tick.isFinished
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            currentTimerId = event.timerId,
                            remainingSeconds = 0,
                            initialSeconds = 0,
                            formattedTime = "00:00",
                            progress = 0f,
                            status = CountdownTimerStatus.IDLE,
                            components = CountdownTimeComponents(),
                            isRunning = false,
                            isFinished = false
                        )
                    }
                }
            }
            is CountdownTimerEvent.ClearAll -> {
                clearAllCountdownTimersUseCase()
                _uiState.update {
                    CountdownTimerUiState()
                }
            }
            is CountdownTimerEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }
}
