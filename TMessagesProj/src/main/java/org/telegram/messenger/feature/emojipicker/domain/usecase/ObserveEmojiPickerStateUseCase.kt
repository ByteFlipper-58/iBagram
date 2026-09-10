package org.telegram.messenger.feature.emojipicker.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.emojipicker.domain.model.EmojiPickerState
import org.telegram.messenger.feature.emojipicker.domain.repository.EmojiPickerRepository

class ObserveEmojiPickerStateUseCase(
    private val repository: EmojiPickerRepository
) {
    operator fun invoke(): StateFlow<EmojiPickerState> = repository.observeState()
}
