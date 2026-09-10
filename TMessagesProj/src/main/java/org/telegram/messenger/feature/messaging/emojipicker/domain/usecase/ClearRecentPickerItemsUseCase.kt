package org.telegram.messenger.feature.messaging.emojipicker.domain.usecase

import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerTabType
import org.telegram.messenger.feature.messaging.emojipicker.domain.repository.EmojiPickerRepository

class ClearRecentPickerItemsUseCase(
    private val repository: EmojiPickerRepository
) {
    operator fun invoke(tab: EmojiPickerTabType) {
        repository.clearRecent(tab)
    }
}
