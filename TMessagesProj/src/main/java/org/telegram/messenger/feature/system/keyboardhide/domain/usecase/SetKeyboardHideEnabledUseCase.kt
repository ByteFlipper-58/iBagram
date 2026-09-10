package org.telegram.messenger.feature.system.keyboardhide.domain.usecase

import org.telegram.messenger.feature.system.keyboardhide.domain.repository.KeyboardHideRepository

class SetKeyboardHideEnabledUseCase(
    private val repository: KeyboardHideRepository
) {
    operator fun invoke(enabled: Boolean) {
        repository.setEnabled(enabled)
    }
}
