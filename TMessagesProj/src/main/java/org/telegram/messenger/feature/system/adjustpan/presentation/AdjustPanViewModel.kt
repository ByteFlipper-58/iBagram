package org.telegram.messenger.feature.system.adjustpan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanCalculationSpec
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanTransitionPlan
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.CalculatePanTransitionPlanUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.GetAdjustPanStateUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.ObserveAdjustPanStateUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.ResetAdjustPanUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.SetAdjustPanEnabledUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.StartAdjustPanTransitionUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.StopAdjustPanTransitionUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.UpdateAdjustPanTransitionUseCase

class AdjustPanViewModel(
    private val calculatePlanUseCase: CalculatePanTransitionPlanUseCase,
    private val observeStateUseCase: ObserveAdjustPanStateUseCase,
    private val getAdjustPanStateUseCase: GetAdjustPanStateUseCase,
    private val setEnabledUseCase: SetAdjustPanEnabledUseCase,
    private val startTransitionUseCase: StartAdjustPanTransitionUseCase,
    private val updateTransitionUseCase: UpdateAdjustPanTransitionUseCase,
    private val stopTransitionUseCase: StopAdjustPanTransitionUseCase,
    private val resetUseCase: ResetAdjustPanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdjustPanUiState(
            transitionState = getAdjustPanStateUseCase(),
            activePlan = PanTransitionPlan.NO_ANIMATION
        )
    )
    val uiState: StateFlow<AdjustPanUiState> = _uiState.asStateFlow()

    init {
        observeStateUseCase()
            .onEach { state ->
                _uiState.update { it.copy(transitionState = state) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: AdjustPanEvent) {
        when (event) {
            is AdjustPanEvent.SetEnabled -> setEnabledUseCase(event.enabled)
            is AdjustPanEvent.PrepareTransition -> prepareTransition(event.spec)
            is AdjustPanEvent.UpdateProgress -> updateProgress(event.progress)
            is AdjustPanEvent.CompleteTransition -> stopTransition(event.progress, event.isKeyboardVisible)
            AdjustPanEvent.Reset -> reset()
        }
    }

    fun prepareTransition(spec: PanCalculationSpec): PanTransitionPlan {
        val plan = calculatePlanUseCase(spec)
        _uiState.update { it.copy(activePlan = plan) }
        if (plan.shouldAnimate) {
            startTransitionUseCase(plan)
        }
        return plan
    }

    fun updateProgress(progress: Float) {
        updateTransitionUseCase(progress)
    }

    fun stopTransition(progress: Float = 0f, isKeyboardVisible: Boolean = false) {
        stopTransitionUseCase(progress, isKeyboardVisible)
        _uiState.update { it.copy(activePlan = PanTransitionPlan.NO_ANIMATION) }
    }

    fun reset() {
        resetUseCase()
        _uiState.update { it.copy(activePlan = PanTransitionPlan.NO_ANIMATION) }
    }
}
