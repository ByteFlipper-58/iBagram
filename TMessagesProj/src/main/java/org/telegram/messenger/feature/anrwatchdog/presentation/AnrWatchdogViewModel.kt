package org.telegram.messenger.feature.anrwatchdog.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.AcknowledgePingUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.CheckMainThreadFreezeUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.ClearAnrHistoryUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.GetAnrIncidentsUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.GetAnrWatchdogStateUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.ObserveAnrIncidentsUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.ObserveAnrWatchdogStateUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.ResolveIncidentUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.SendMainThreadPingUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.SetAppForegroundStatusUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.StartAnrMonitoringUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.StopAnrMonitoringUseCase

/**
 * ViewModel for inspecting and controlling the ANR watchdog.
 */
class AnrWatchdogViewModel(
    private val startAnrMonitoringUseCase: StartAnrMonitoringUseCase,
    private val stopAnrMonitoringUseCase: StopAnrMonitoringUseCase,
    private val setAppForegroundStatusUseCase: SetAppForegroundStatusUseCase,
    private val sendMainThreadPingUseCase: SendMainThreadPingUseCase,
    private val acknowledgePingUseCase: AcknowledgePingUseCase,
    private val checkMainThreadFreezeUseCase: CheckMainThreadFreezeUseCase,
    private val resolveIncidentUseCase: ResolveIncidentUseCase,
    private val getAnrWatchdogStateUseCase: GetAnrWatchdogStateUseCase,
    private val getAnrIncidentsUseCase: GetAnrIncidentsUseCase,
    private val clearAnrHistoryUseCase: ClearAnrHistoryUseCase,
    private val observeAnrWatchdogStateUseCase: ObserveAnrWatchdogStateUseCase,
    private val observeAnrIncidentsUseCase: ObserveAnrIncidentsUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {

    private val _uiState = MutableStateFlow(
        AnrWatchdogUiState(
            state = getAnrWatchdogStateUseCase(),
            incidents = getAnrIncidentsUseCase()
        )
    )
    val uiState: StateFlow<AnrWatchdogUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            observeAnrWatchdogStateUseCase().collectLatest { state ->
                _uiState.update { current ->
                    current.copy(
                        state = state,
                        incidents = getAnrIncidentsUseCase()
                    )
                }
            }
        }

        scope.launch {
            observeAnrIncidentsUseCase().collect { incident ->
                _uiState.update { current ->
                    val updatedList = (listOf(incident) + current.incidents).distinctBy { it.incidentId }
                    current.copy(incidents = updatedList)
                }
            }
        }
    }

    fun onEvent(event: AnrWatchdogEvent) {
        when (event) {
            is AnrWatchdogEvent.StartMonitoring -> {
                startAnrMonitoringUseCase()
            }
            is AnrWatchdogEvent.StopMonitoring -> {
                stopAnrMonitoringUseCase()
            }
            is AnrWatchdogEvent.SetForeground -> {
                setAppForegroundStatusUseCase(event.isForeground)
            }
            is AnrWatchdogEvent.SendPing -> {
                sendMainThreadPingUseCase(event.timestampNanos)
            }
            is AnrWatchdogEvent.AcknowledgePing -> {
                acknowledgePingUseCase(event.pingId)
            }
            is AnrWatchdogEvent.CheckFreeze -> {
                checkMainThreadFreezeUseCase(event.nowNanos, event.timeoutMs)
            }
            is AnrWatchdogEvent.ResolveIncident -> {
                resolveIncidentUseCase(event.incidentId)
            }
            is AnrWatchdogEvent.ClearHistory -> {
                clearAnrHistoryUseCase()
                _uiState.update { it.copy(incidents = emptyList()) }
            }
            is AnrWatchdogEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
