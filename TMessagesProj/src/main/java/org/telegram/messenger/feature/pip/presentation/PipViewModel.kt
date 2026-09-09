package org.telegram.messenger.feature.pip.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.pip.domain.model.PipSessionInfo
import org.telegram.messenger.feature.pip.domain.usecase.DispatchPipStateUseCase
import org.telegram.messenger.feature.pip.domain.usecase.EvaluatePipEligibilityUseCase
import org.telegram.messenger.feature.pip.domain.usecase.GetPipSessionUseCase
import org.telegram.messenger.feature.pip.domain.usecase.ObservePipSessionUseCase
import org.telegram.messenger.feature.pip.domain.usecase.RegisterPipSourceUseCase
import org.telegram.messenger.feature.pip.domain.usecase.TriggerPipActionUseCase
import org.telegram.messenger.feature.pip.domain.usecase.UnregisterPipSourceUseCase
import org.telegram.messenger.feature.pip.domain.usecase.UpdatePipSourceStateUseCase

class PipViewModel(
    private val observePipSessionUseCase: ObservePipSessionUseCase,
    private val getPipSessionUseCase: GetPipSessionUseCase,
    private val registerPipSourceUseCase: RegisterPipSourceUseCase,
    private val unregisterPipSourceUseCase: UnregisterPipSourceUseCase,
    private val updatePipSourceStateUseCase: UpdatePipSourceStateUseCase,
    private val dispatchPipStateUseCase: DispatchPipStateUseCase,
    private val triggerPipActionUseCase: TriggerPipActionUseCase,
    private val evaluatePipEligibilityUseCase: EvaluatePipEligibilityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PipUiState(isLoading = true))
    val uiState: StateFlow<PipUiState> = _uiState.asStateFlow()

    init {
        observePipSessionUseCase()
            .onEach { session ->
                updateUiState(session)
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: PipEvent) {
        when (event) {
            is PipEvent.RegisterSource -> {
                registerPipSourceUseCase(event.source)
            }
            is PipEvent.UnregisterSource -> {
                unregisterPipSourceUseCase(event.tag)
            }
            is PipEvent.SetSourceAvailability -> {
                updatePipSourceStateUseCase.setAvailability(event.tag, event.isAvailable)
            }
            is PipEvent.SetSourceRatio -> {
                updatePipSourceStateUseCase.setAspectRatio(event.tag, event.width, event.height)
            }
            is PipEvent.SetSourceAttached -> {
                updatePipSourceStateUseCase.setAttached(event.tag, event.isAttached)
            }
            is PipEvent.TransitionPipState -> {
                dispatchPipStateUseCase(event.state, event.byActivityStop)
            }
            is PipEvent.TriggerAction -> {
                triggerPipActionUseCase(event.tag, event.actionId)
            }
            is PipEvent.Refresh -> {
                val session = getPipSessionUseCase()
                updateUiState(session)
            }
        }
    }

    private fun updateUiState(session: PipSessionInfo) {
        val canEnter = evaluatePipEligibilityUseCase()
        _uiState.value = PipUiState(
            isLoading = false,
            pipState = session.pipState,
            activeSource = session.activeSource,
            registeredSources = session.registeredSources,
            registeredSourceCount = session.registeredSources.size,
            canEnterPip = canEnter,
            isMediaSessionActive = session.isMediaSessionActive,
            lastAction = session.lastAction,
            errorMessage = null
        )
    }
}
