package org.telegram.messenger.feature.refreshrate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.refreshrate.domain.model.DisplayRefreshModeModel
import org.telegram.messenger.feature.refreshrate.domain.repository.RefreshRateRepository

class SetPreferredRefreshRateModeUseCase(
    private val repository: RefreshRateRepository
) {
    operator fun invoke(mode: DisplayRefreshModeModel): Result<Unit> {
        return repository.setPreferredMode(mode)
    }
}
