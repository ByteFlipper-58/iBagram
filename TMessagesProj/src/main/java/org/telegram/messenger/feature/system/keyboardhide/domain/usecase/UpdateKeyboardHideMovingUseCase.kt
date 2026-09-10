package org.telegram.messenger.feature.system.keyboardhide.domain.usecase

import org.telegram.messenger.feature.system.keyboardhide.domain.repository.KeyboardHideRepository

class UpdateKeyboardHideMovingUseCase(
    private val repository: KeyboardHideRepository
) {
    operator fun invoke(rawProgress: Float, progress: Float, translationY: Float) {
        repository.updateMoving(rawProgress, progress, translationY)
    }
}
