package org.telegram.messenger.feature.adjustpan.domain.usecase

import org.telegram.messenger.feature.adjustpan.domain.repository.AdjustPanRepository

class SetAdjustPanEnabledUseCase(
    private val repository: AdjustPanRepository
) {
    operator fun invoke(enabled: Boolean) {
        repository.setEnabled(enabled)
    }
}
