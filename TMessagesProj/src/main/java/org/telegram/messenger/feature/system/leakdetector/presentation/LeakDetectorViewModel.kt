package org.telegram.messenger.feature.system.leakdetector.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.system.leakdetector.domain.usecase.ConfirmLeakUseCase
import org.telegram.messenger.feature.system.leakdetector.domain.usecase.GetConfirmedLeaksUseCase
import org.telegram.messenger.feature.system.leakdetector.domain.usecase.GetTrackedClassesStatsUseCase
import org.telegram.messenger.feature.system.leakdetector.domain.usecase.ObserveLeakDetectorStateUseCase
import org.telegram.messenger.feature.system.leakdetector.domain.usecase.ResetLeakDetectorUseCase
import org.telegram.messenger.feature.system.leakdetector.domain.usecase.StartLeakDetectionUseCase
import org.telegram.messenger.feature.system.leakdetector.domain.usecase.StopLeakDetectionUseCase
import org.telegram.messenger.feature.system.leakdetector.domain.usecase.TrackInstanceUseCase
import org.telegram.messenger.feature.system.leakdetector.domain.usecase.TriggerLeakCheckUseCase

class LeakDetectorViewModel(
    private val startLeakDetectionUseCase: StartLeakDetectionUseCase,
    private val stopLeakDetectionUseCase: StopLeakDetectionUseCase,
    private val trackInstanceUseCase: TrackInstanceUseCase,
    private val triggerLeakCheckUseCase: TriggerLeakCheckUseCase,
    private val confirmLeakUseCase: ConfirmLeakUseCase,
    private val getTrackedClassesStatsUseCase: GetTrackedClassesStatsUseCase,
    private val getConfirmedLeaksUseCase: GetConfirmedLeaksUseCase,
    private val resetLeakDetectorUseCase: ResetLeakDetectorUseCase,
    private val observeLeakDetectorStateUseCase: ObserveLeakDetectorStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeakDetectorUiState())
    val uiState: StateFlow<LeakDetectorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeLeakDetectorStateUseCase().collectLatest { state ->
                val stats = getTrackedClassesStatsUseCase()
                _uiState.update { current ->
                    current.copy(
                        isRunning = state.isRunning,
                        trackedClassesCount = state.trackedClassesCount,
                        totalLiveInstances = state.totalLiveInstances,
                        suspiciousClassesCount = state.suspiciousClassesCount,
                        confirmedLeaks = state.confirmedLeaks,
                        trackedStats = stats
                    )
                }
            }
        }
    }

    fun onEvent(event: LeakDetectorEvent) {
        when (event) {
            is LeakDetectorEvent.Start -> {
                startLeakDetectionUseCase(event.config)
            }
            is LeakDetectorEvent.Stop -> {
                stopLeakDetectionUseCase()
            }
            is LeakDetectorEvent.Track -> {
                trackInstanceUseCase(event.tagOrClassName, event.instance)
            }
            is LeakDetectorEvent.TriggerCheck -> {
                _uiState.update { it.copy(isChecking = true) }
                try {
                    triggerLeakCheckUseCase()
                } finally {
                    _uiState.update { it.copy(isChecking = false) }
                }
            }
            is LeakDetectorEvent.Confirm -> {
                confirmLeakUseCase(event.className)
            }
            is LeakDetectorEvent.Reset -> {
                resetLeakDetectorUseCase()
                _uiState.update { LeakDetectorUiState() }
            }
            is LeakDetectorEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }
}
