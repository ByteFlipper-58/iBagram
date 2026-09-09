package org.telegram.messenger.feature.keyboardinsets.domain.usecase

import org.telegram.messenger.feature.keyboardinsets.domain.repository.KeyboardInsetsRepository

class RequestInAppKeyboardHeightWithNavbarUseCase(
    private val repository: KeyboardInsetsRepository
) {
    operator fun invoke(height: Int, navigationBarHeight: Int) {
        repository.requestInAppKeyboardHeightIncludeNavbar(height, navigationBarHeight)
    }
}
