package org.telegram.messenger.feature.messaging.emojipicker.domain.usecase

import org.telegram.messenger.feature.messaging.emojipicker.domain.repository.EmojiPickerRepository

class ToggleStickerFavoriteUseCase(
    private val repository: EmojiPickerRepository
) {
    operator fun invoke(stickerId: Long) {
        repository.toggleStickerFavorite(stickerId)
    }
}
