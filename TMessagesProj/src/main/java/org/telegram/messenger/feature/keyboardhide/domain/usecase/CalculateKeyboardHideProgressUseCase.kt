package org.telegram.messenger.feature.keyboardhide.domain.usecase

import org.telegram.messenger.feature.keyboardhide.domain.model.KeyboardDragSpec
import org.telegram.messenger.feature.keyboardhide.domain.model.KeyboardHideProgressResult
import org.telegram.messenger.feature.keyboardhide.domain.repository.KeyboardHideRepository

class CalculateKeyboardHideProgressUseCase(
    private val repository: KeyboardHideRepository
) {
    operator fun invoke(spec: KeyboardDragSpec): KeyboardHideProgressResult {
        return repository.calculateProgress(spec)
    }
}
