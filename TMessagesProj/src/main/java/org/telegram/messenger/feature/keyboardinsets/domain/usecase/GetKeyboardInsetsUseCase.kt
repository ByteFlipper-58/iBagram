package org.telegram.messenger.feature.keyboardinsets.domain.usecase

import org.telegram.messenger.feature.keyboardinsets.domain.model.KeyboardInsetsModel
import org.telegram.messenger.feature.keyboardinsets.domain.repository.KeyboardInsetsRepository

class GetKeyboardInsetsUseCase(
    private val repository: KeyboardInsetsRepository
) {
    operator fun invoke(): KeyboardInsetsModel = repository.getKeyboardInsets()
}
