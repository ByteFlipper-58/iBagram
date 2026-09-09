package org.telegram.messenger.feature.refreshrate.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.refreshrate.domain.model.RefreshRateStateModel
import org.telegram.messenger.feature.refreshrate.domain.repository.RefreshRateRepository

class ObserveRefreshRateStateUseCase(
    private val repository: RefreshRateRepository
) {
    operator fun invoke(): Flow<RefreshRateStateModel> {
        return repository.observeState()
    }
}
