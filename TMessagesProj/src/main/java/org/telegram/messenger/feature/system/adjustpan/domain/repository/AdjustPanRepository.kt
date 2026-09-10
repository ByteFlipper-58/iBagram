package org.telegram.messenger.feature.system.adjustpan.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanCalculationSpec
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanProgressResult
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanTransitionPlan
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanTransitionState

/**
 * Repository interface for calculating adjust pan geometry and arbitrating layout transitions.
 */
interface AdjustPanRepository {

    /**
     * Calculates the pan transition plan according to incoming geometry specifications.
     */
    fun calculatePlan(spec: PanCalculationSpec): PanTransitionPlan

    /**
     * Computes the interpolated translation result for a given normalized progress (0.0 to 1.0).
     */
    fun computeProgress(plan: PanTransitionPlan, progress: Float): PanProgressResult

    /**
     * Observes real-time state changes of the adjust-pan controller.
     */
    fun observeState(): StateFlow<PanTransitionState>

    /**
     * Returns a snapshot of the current adjust-pan state.
     */
    fun getState(): PanTransitionState

    /**
     * Enables or disables height adjustment animations.
     */
    fun setEnabled(enabled: Boolean)

    /**
     * Signals the start of a pan transition with the calculated plan.
     */
    fun startTransition(plan: PanTransitionPlan)

    /**
     * Updates ongoing transition progress (0.0 to 1.0).
     */
    fun updateTransition(progress: Float)

    /**
     * Finalizes and stops transition, restoring views.
     */
    fun stopTransition(progress: Float = 0f, isKeyboardVisible: Boolean = false)

    /**
     * Resets the repository state to defaults.
     */
    fun reset()
}
