package org.telegram.messenger.feature.messaging.emojieffects.domain.usecase

import org.telegram.messenger.feature.messaging.emojieffects.domain.repository.EmojiEffectsRepository

class ClearEmojiEffectsUseCase(
    private val repository: EmojiEffectsRepository
) {
    operator fun invoke() {
        repository.clear()
    }
}
