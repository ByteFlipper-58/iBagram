package org.telegram.messenger.feature.system.adjustpan.domain.usecase

import org.telegram.messenger.feature.system.adjustpan.domain.repository.AdjustPanRepository

class SetAdjustPanEnabledUseCase(
    private val repository: AdjustPanRepository
) {
    operator fun invoke(enabled: Boolean) {
        repository.setEnabled(enabled)
    }
}
