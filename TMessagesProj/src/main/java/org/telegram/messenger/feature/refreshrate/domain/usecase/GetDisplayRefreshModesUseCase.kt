package org.telegram.messenger.feature.refreshrate.domain.usecase

import org.telegram.messenger.feature.refreshrate.domain.model.DisplayRefreshModeModel
import org.telegram.messenger.feature.refreshrate.domain.repository.RefreshRateRepository

class GetDisplayRefreshModesUseCase(
    private val repository: RefreshRateRepository
) {
    operator fun invoke(): List<DisplayRefreshModeModel> {
        return repository.getAvailableModes()
    }
}
