package org.telegram.messenger.feature.refreshrate.domain.usecase

import org.telegram.messenger.feature.refreshrate.domain.model.RefreshRateStateModel
import org.telegram.messenger.feature.refreshrate.domain.repository.RefreshRateRepository

class GetRefreshRateStateUseCase(
    private val repository: RefreshRateRepository
) {
    operator fun invoke(): RefreshRateStateModel {
        return repository.getState()
    }
}
