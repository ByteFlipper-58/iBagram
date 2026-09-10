package org.telegram.messenger.feature.messaging.emojipicker.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerState
import org.telegram.messenger.feature.messaging.emojipicker.domain.repository.EmojiPickerRepository

class ObserveEmojiPickerStateUseCase(
    private val repository: EmojiPickerRepository
) {
    operator fun invoke(): StateFlow<EmojiPickerState> = repository.observeState()
}
