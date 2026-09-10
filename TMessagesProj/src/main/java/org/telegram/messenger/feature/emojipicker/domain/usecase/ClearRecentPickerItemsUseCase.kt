package org.telegram.messenger.feature.emojipicker.domain.usecase

import org.telegram.messenger.feature.emojipicker.domain.model.EmojiPickerTabType
import org.telegram.messenger.feature.emojipicker.domain.repository.EmojiPickerRepository

class ClearRecentPickerItemsUseCase(
    private val repository: EmojiPickerRepository
) {
    operator fun invoke(tab: EmojiPickerTabType) {
        repository.clearRecent(tab)
    }
}
