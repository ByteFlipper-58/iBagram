package org.telegram.messenger.feature.system.refreshrate.domain.usecase

import org.telegram.messenger.feature.system.refreshrate.domain.model.DisplayRefreshModeModel
import org.telegram.messenger.feature.system.refreshrate.domain.repository.RefreshRateRepository

class GetDisplayRefreshModesUseCase(
    private val repository: RefreshRateRepository
) {
    operator fun invoke(): List<DisplayRefreshModeModel> {
        return repository.getAvailableModes()
    }
}
