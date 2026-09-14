package org.telegram.messenger.feature.system.adjustpan.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.system.adjustpan.data.mapper.AdjustPanMapper
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanCalculationSpec
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanProgressResult
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanTransitionPlan
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanTransitionState

open class AdjustPanLocalDataSource {

    private val _state = MutableStateFlow(PanTransitionState())
    private val state: StateFlow<PanTransitionState> = _state.asStateFlow()

    @Volatile
    private var currentPlan: PanTransitionPlan = PanTransitionPlan.NO_ANIMATION

    open fun calculatePlan(spec: PanCalculationSpec): PanTransitionPlan {
        val effectiveSpec = if (!_state.value.isEnabled) {
            spec.copy(isHeightAnimationEnabled = false)
        } else {
            spec
        }
        val plan = AdjustPanMapper.calculatePlan(effectiveSpec)
        currentPlan = plan
        return plan
    }

    open fun computeProgress(plan: PanTransitionPlan, progress: Float): PanProgressResult {
        return AdjustPanMapper.interpolateProgress(plan, progress)
    }

    open fun observeState(): StateFlow<PanTransitionState> = state

    open fun getState(): PanTransitionState = _state.value

    open fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(isEnabled = enabled) }
    }

    open fun startTransition(plan: PanTransitionPlan) {
        currentPlan = plan
        val initial = AdjustPanMapper.interpolateProgress(plan, 0f)
        _state.update {
            it.copy(
                isAnimationInProgress = plan.shouldAnimate,
                isShowingKeyboard = plan.showingKeyboard,
                isKeyboardVisible = plan.isKeyboardVisible,
                keyboardSize = plan.keyboardSize,
                targetHeight = plan.targetHeight,
                currentTranslationY = initial.translationY,
                currentProgress = 0f
            )
        }
    }

    open fun updateTransition(progress: Float) {
        val result = AdjustPanMapper.interpolateProgress(currentPlan, progress)
        _state.update {
            it.copy(
                currentTranslationY = result.translationY,
                currentProgress = progress,
                isKeyboardVisible = result.isKeyboardVisible
            )
        }
    }

    open fun stopTransition(progress: Float = 0f, isKeyboardVisible: Boolean = false) {
        val lastKeyboardVisible = if (currentPlan.shouldAnimate) currentPlan.isKeyboardVisible else isKeyboardVisible
        currentPlan = PanTransitionPlan.NO_ANIMATION
        _state.update {
            it.copy(
                isAnimationInProgress = false,
                currentTranslationY = 0f,
                currentProgress = progress,
                isKeyboardVisible = lastKeyboardVisible,
                targetHeight = -1
            )
        }
    }

    open fun reset() {
        currentPlan = PanTransitionPlan.NO_ANIMATION
        _state.value = PanTransitionState()
    }
}
