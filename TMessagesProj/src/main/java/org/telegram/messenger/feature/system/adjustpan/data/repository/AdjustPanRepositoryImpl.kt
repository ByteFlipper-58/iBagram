package org.telegram.messenger.feature.system.adjustpan.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.adjustpan.data.datasource.AdjustPanLocalDataSource
import org.telegram.messenger.feature.system.adjustpan.data.datasource.AdjustPanRemoteDataSource
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanCalculationSpec
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanProgressResult
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanTransitionPlan
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanTransitionState
import org.telegram.messenger.feature.system.adjustpan.domain.repository.AdjustPanRepository

class AdjustPanRepositoryImpl(
    private val localDataSource: AdjustPanLocalDataSource,
    private val remoteDataSource: AdjustPanRemoteDataSource
) : AdjustPanRepository {

    override fun calculatePlan(spec: PanCalculationSpec): PanTransitionPlan {
        return localDataSource.calculatePlan(spec)
    }

    override fun computeProgress(plan: PanTransitionPlan, progress: Float): PanProgressResult {
        return localDataSource.computeProgress(plan, progress)
    }

    override fun observeState(): StateFlow<PanTransitionState> = localDataSource.observeState()

    override fun getState(): PanTransitionState = localDataSource.getState()

    override fun setEnabled(enabled: Boolean) {
        localDataSource.setEnabled(enabled)
    }

    override fun startTransition(plan: PanTransitionPlan) {
        localDataSource.startTransition(plan)
    }

    override fun updateTransition(progress: Float) {
        localDataSource.updateTransition(progress)
    }

    override fun stopTransition(progress: Float, isKeyboardVisible: Boolean) {
        localDataSource.stopTransition(progress, isKeyboardVisible)
    }

    override fun reset() {
        localDataSource.reset()
    }
}
