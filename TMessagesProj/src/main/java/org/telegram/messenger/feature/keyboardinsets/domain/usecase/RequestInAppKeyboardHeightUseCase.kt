package org.telegram.messenger.feature.keyboardinsets.domain.usecase

import org.telegram.messenger.feature.keyboardinsets.domain.repository.KeyboardInsetsRepository

class RequestInAppKeyboardHeightUseCase(
    private val repository: KeyboardInsetsRepository
) {
    operator fun invoke(height: Int) {
        repository.requestInAppKeyboardHeight(height)
    }
}
