package org.telegram.messenger.feature.system.refreshrate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.refreshrate.domain.repository.RefreshRateRepository

class ToggleAdaptiveRefreshRateUseCase(
    private val repository: RefreshRateRepository
) {
    operator fun invoke(enabled: Boolean): Result<Unit> {
        return repository.setAdaptiveEnabled(enabled)
    }
}
