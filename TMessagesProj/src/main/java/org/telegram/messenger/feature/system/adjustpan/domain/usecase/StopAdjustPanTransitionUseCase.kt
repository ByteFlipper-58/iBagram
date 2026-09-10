package org.telegram.messenger.feature.system.adjustpan.domain.usecase

import org.telegram.messenger.feature.system.adjustpan.domain.repository.AdjustPanRepository

class StopAdjustPanTransitionUseCase(
    private val repository: AdjustPanRepository
) {
    operator fun invoke(progress: Float = 0f, isKeyboardVisible: Boolean = false) {
        repository.stopTransition(progress, isKeyboardVisible)
    }
}
