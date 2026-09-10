package org.telegram.messenger.feature.system.keyboardhide.domain.usecase

import org.telegram.messenger.feature.system.keyboardhide.domain.repository.KeyboardHideRepository

class ResetKeyboardHideUseCase(
    private val repository: KeyboardHideRepository
) {
    operator fun invoke() {
        repository.reset()
    }
}
