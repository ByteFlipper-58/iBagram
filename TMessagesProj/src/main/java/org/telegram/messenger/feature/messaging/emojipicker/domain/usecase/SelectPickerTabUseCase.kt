package org.telegram.messenger.feature.messaging.emojipicker.domain.usecase

import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerTabType
import org.telegram.messenger.feature.messaging.emojipicker.domain.repository.EmojiPickerRepository

class SelectPickerTabUseCase(
    private val repository: EmojiPickerRepository
) {
    operator fun invoke(tab: EmojiPickerTabType) {
        val state = repository.getState()
        if (state.availableTabs.contains(tab)) {
            repository.selectTab(tab)
        }
    }
}
