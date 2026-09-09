package org.telegram.messenger.feature.keyboardinsets.domain.usecase

import org.telegram.messenger.feature.keyboardinsets.domain.repository.KeyboardInsetsRepository

class UpdateSystemInsetsUseCase(
    private val repository: KeyboardInsetsRepository
) {
    operator fun invoke(top: Int, bottom: Int, imeBottom: Int, animated: Boolean = true) {
        repository.updateSystemInsets(top, bottom, imeBottom, animated)
    }
}
