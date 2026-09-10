package org.telegram.messenger.feature.system.keyboardhide.domain.usecase

import org.telegram.messenger.feature.system.keyboardhide.domain.repository.KeyboardHideRepository

class FinishKeyboardHideDismissUseCase(
    private val repository: KeyboardHideRepository
) {
    operator fun invoke(dismissed: Boolean) {
        repository.finishDismiss(dismissed)
    }
}
