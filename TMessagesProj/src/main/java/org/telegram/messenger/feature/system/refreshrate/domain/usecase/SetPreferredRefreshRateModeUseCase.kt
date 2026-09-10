package org.telegram.messenger.feature.system.refreshrate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.refreshrate.domain.model.DisplayRefreshModeModel
import org.telegram.messenger.feature.system.refreshrate.domain.repository.RefreshRateRepository

class SetPreferredRefreshRateModeUseCase(
    private val repository: RefreshRateRepository
) {
    operator fun invoke(mode: DisplayRefreshModeModel): Result<Unit> {
        return repository.setPreferredMode(mode)
    }
}
