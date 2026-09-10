package org.telegram.messenger.feature.system.keyboardinsets.domain.usecase

import org.telegram.messenger.feature.system.keyboardinsets.domain.model.KeyboardInsetsModel
import org.telegram.messenger.feature.system.keyboardinsets.domain.repository.KeyboardInsetsRepository

class GetKeyboardInsetsUseCase(
    private val repository: KeyboardInsetsRepository
) {
    operator fun invoke(): KeyboardInsetsModel = repository.getKeyboardInsets()
}
