package org.telegram.messenger.feature.keyboardhide.domain.usecase

import org.telegram.messenger.feature.keyboardhide.domain.repository.KeyboardHideRepository

class FinishKeyboardHideDismissUseCase(
    private val repository: KeyboardHideRepository
) {
    operator fun invoke(dismissed: Boolean) {
        repository.finishDismiss(dismissed)
    }
}
