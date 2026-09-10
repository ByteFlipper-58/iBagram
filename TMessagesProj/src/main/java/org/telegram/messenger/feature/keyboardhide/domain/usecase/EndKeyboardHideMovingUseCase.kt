package org.telegram.messenger.feature.keyboardhide.domain.usecase

import org.telegram.messenger.feature.keyboardhide.domain.repository.KeyboardHideRepository

class EndKeyboardHideMovingUseCase(
    private val repository: KeyboardHideRepository
) {
    operator fun invoke(shouldDismiss: Boolean) {
        repository.endMoving(shouldDismiss)
    }
}
