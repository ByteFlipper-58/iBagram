package org.telegram.messenger.feature.emojieffects.domain.usecase

import org.telegram.messenger.feature.emojieffects.domain.repository.EmojiEffectsRepository

class ClearEmojiEffectsUseCase(
    private val repository: EmojiEffectsRepository
) {
    operator fun invoke() {
        repository.clear()
    }
}
