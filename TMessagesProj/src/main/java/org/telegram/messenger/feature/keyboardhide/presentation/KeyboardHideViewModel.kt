package org.telegram.messenger.feature.keyboardhide.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.keyboardhide.domain.model.KeyboardDismissDecision
import org.telegram.messenger.feature.keyboardhide.domain.model.KeyboardDragSpec
import org.telegram.messenger.feature.keyboardhide.domain.model.KeyboardHideProgressResult
import org.telegram.messenger.feature.keyboardhide.domain.usecase.CalculateKeyboardHideProgressUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.EndKeyboardHideMovingUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.EvaluateKeyboardDismissDecisionUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.FinishKeyboardHideDismissUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.GetKeyboardHideStateUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.ObserveKeyboardHideStateUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.ResetKeyboardHideUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.SetKeyboardHideEnabledUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.StartKeyboardHideMovingUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.UpdateKeyboardHideMovingUseCase

class KeyboardHideViewModel(
    private val calculateProgressUseCase: CalculateKeyboardHideProgressUseCase,
    private val evaluateDismissDecisionUseCase: EvaluateKeyboardDismissDecisionUseCase,
    private val observeStateUseCase: ObserveKeyboardHideStateUseCase,
    private val getStateUseCase: GetKeyboardHideStateUseCase,
    private val setEnabledUseCase: SetKeyboardHideEnabledUseCase,
    private val startMovingUseCase: StartKeyboardHideMovingUseCase,
    private val updateMovingUseCase: UpdateKeyboardHideMovingUseCase,
    private val endMovingUseCase: EndKeyboardHideMovingUseCase,
    private val finishDismissUseCase: FinishKeyboardHideDismissUseCase,
    private val resetUseCase: ResetKeyboardHideUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        KeyboardHideUiState(
            hideState = getStateUseCase(),
            isDragging = false
        )
    )
    val uiState: StateFlow<KeyboardHideUiState> = _uiState.asStateFlow()

    init {
        observeStateUseCase()
            .onEach { state ->
                _uiState.update {
                    it.copy(
                        hideState = state,
                        isDragging = state.isMovingKeyboard
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: KeyboardHideEvent) {
        when (event) {
            is KeyboardHideEvent.SetEnabled -> setEnabledUseCase(event.enabled)
            is KeyboardHideEvent.StartDrag -> startMovingUseCase(
                event.keyboardSize,
                event.bottomNavBarSize,
                event.isKeyboard
            )
            is KeyboardHideEvent.UpdateDrag -> updateMovingUseCase(
                event.rawProgress,
                event.progress,
                event.translationY
            )
            is KeyboardHideEvent.EndDrag -> endMovingUseCase(event.shouldDismiss)
            is KeyboardHideEvent.FinishDismiss -> finishDismissUseCase(event.dismissed)
            KeyboardHideEvent.Reset -> resetUseCase()
        }
    }

    fun calculateProgress(spec: KeyboardDragSpec): KeyboardHideProgressResult {
        return calculateProgressUseCase(spec)
    }

    fun evaluateDismissDecision(
        currentProgress: Float,
        lastDifferentProgress: Float,
        velocityY: Float = 0f
    ): KeyboardDismissDecision {
        return evaluateDismissDecisionUseCase(currentProgress, lastDifferentProgress, velocityY)
    }
}
