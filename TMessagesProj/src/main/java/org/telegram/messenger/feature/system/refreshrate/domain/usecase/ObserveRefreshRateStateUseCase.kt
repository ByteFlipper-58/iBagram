package org.telegram.messenger.feature.system.refreshrate.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.refreshrate.domain.model.RefreshRateStateModel
import org.telegram.messenger.feature.system.refreshrate.domain.repository.RefreshRateRepository

class ObserveRefreshRateStateUseCase(
    private val repository: RefreshRateRepository
) {
    operator fun invoke(): Flow<RefreshRateStateModel> {
        return repository.observeState()
    }
}
