package org.telegram.messenger.feature.refreshrate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.refreshrate.domain.repository.RefreshRateRepository

class StopRefreshRateTrackingUseCase(
    private val repository: RefreshRateRepository
) {
    operator fun invoke(): Result<Unit> {
        return repository.stopTracking()
    }
}
