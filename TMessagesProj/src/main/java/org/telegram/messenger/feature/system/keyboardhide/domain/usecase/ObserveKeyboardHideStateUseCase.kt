package org.telegram.messenger.feature.system.keyboardhide.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardHideState
import org.telegram.messenger.feature.system.keyboardhide.domain.repository.KeyboardHideRepository

class ObserveKeyboardHideStateUseCase(
    private val repository: KeyboardHideRepository
) {
    operator fun invoke(): StateFlow<KeyboardHideState> {
        return repository.observeState()
    }
}
