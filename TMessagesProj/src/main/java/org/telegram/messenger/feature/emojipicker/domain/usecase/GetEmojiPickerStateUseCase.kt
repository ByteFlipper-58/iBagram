package org.telegram.messenger.feature.emojipicker.domain.usecase

import org.telegram.messenger.feature.emojipicker.domain.model.EmojiPickerState
import org.telegram.messenger.feature.emojipicker.domain.repository.EmojiPickerRepository

class GetEmojiPickerStateUseCase(
    private val repository: EmojiPickerRepository
) {
    operator fun invoke(): EmojiPickerState = repository.getState()
}
