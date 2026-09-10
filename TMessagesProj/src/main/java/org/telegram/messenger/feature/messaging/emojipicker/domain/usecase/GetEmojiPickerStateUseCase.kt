package org.telegram.messenger.feature.messaging.emojipicker.domain.usecase

import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerState
import org.telegram.messenger.feature.messaging.emojipicker.domain.repository.EmojiPickerRepository

class GetEmojiPickerStateUseCase(
    private val repository: EmojiPickerRepository
) {
    operator fun invoke(): EmojiPickerState = repository.getState()
}
