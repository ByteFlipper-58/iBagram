package org.telegram.messenger.feature.emojipicker.domain.usecase

import org.telegram.messenger.feature.emojipicker.domain.model.EmojiPickerTabType
import org.telegram.messenger.feature.emojipicker.domain.repository.EmojiPickerRepository

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
