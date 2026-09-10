package org.telegram.messenger.feature.keyboardhide.domain.usecase

import org.telegram.messenger.feature.keyboardhide.domain.repository.KeyboardHideRepository

class StartKeyboardHideMovingUseCase(
    private val repository: KeyboardHideRepository
) {
    operator fun invoke(keyboardSize: Int, bottomNavBarSize: Int, isKeyboard: Boolean) {
        repository.startMoving(keyboardSize, bottomNavBarSize, isKeyboard)
    }
}
