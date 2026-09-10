package org.telegram.messenger.feature.system.refreshrate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.refreshrate.domain.repository.RefreshRateRepository

class StartRefreshRateTrackingUseCase(
    private val repository: RefreshRateRepository
) {
    operator fun invoke(): Result<Unit> {
        return repository.startTracking()
    }
}
