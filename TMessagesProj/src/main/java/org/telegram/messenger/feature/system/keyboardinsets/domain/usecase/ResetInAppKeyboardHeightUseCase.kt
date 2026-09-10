package org.telegram.messenger.feature.system.keyboardinsets.domain.usecase

import org.telegram.messenger.feature.system.keyboardinsets.domain.repository.KeyboardInsetsRepository

class ResetInAppKeyboardHeightUseCase(
    private val repository: KeyboardInsetsRepository
) {
    operator fun invoke(waitKeyboardOpen: Boolean = true) {
        repository.resetInAppKeyboardHeight(waitKeyboardOpen)
    }
}
