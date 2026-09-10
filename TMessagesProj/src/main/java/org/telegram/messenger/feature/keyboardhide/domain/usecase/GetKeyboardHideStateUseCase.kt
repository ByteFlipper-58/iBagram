package org.telegram.messenger.feature.keyboardhide.domain.usecase

import org.telegram.messenger.feature.keyboardhide.domain.model.KeyboardHideState
import org.telegram.messenger.feature.keyboardhide.domain.repository.KeyboardHideRepository

class GetKeyboardHideStateUseCase(
    private val repository: KeyboardHideRepository
) {
    operator fun invoke(): KeyboardHideState {
        return repository.getState()
    }
}
