package org.telegram.messenger.feature.system.refreshrate.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.refreshrate.domain.model.DisplayRefreshModeModel
import org.telegram.messenger.feature.system.refreshrate.domain.model.RefreshRateStateModel

/**
 * Domain repository contract for adaptive display refresh rate control.
 */
interface RefreshRateRepository {
    fun observeState(): Flow<RefreshRateStateModel>
    fun getState(): RefreshRateStateModel
    fun startTracking(): Result<Unit>
    fun stopTracking(): Result<Unit>
    fun setAdaptiveEnabled(enabled: Boolean): Result<Unit>
    fun setPreferredMode(mode: DisplayRefreshModeModel): Result<Unit>
    fun recordFrameDuration(durationNs: Long): Result<Unit>
    fun resetStats(): Result<Unit>
    fun getAvailableModes(): List<DisplayRefreshModeModel>
}
