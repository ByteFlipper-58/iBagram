package org.telegram.messenger.feature.keyboardinsets.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.keyboardinsets.domain.model.KeyboardInsetsModel
import org.telegram.messenger.feature.keyboardinsets.domain.repository.KeyboardInsetsRepository

class ObserveKeyboardInsetsUseCase(
    private val repository: KeyboardInsetsRepository
) {
    operator fun invoke(): Flow<KeyboardInsetsModel> = repository.observeKeyboardInsets()
}
