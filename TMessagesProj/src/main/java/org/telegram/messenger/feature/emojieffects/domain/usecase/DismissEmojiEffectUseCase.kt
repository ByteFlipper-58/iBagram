package org.telegram.messenger.feature.emojieffects.domain.usecase

import org.telegram.messenger.feature.emojieffects.domain.repository.EmojiEffectsRepository

class DismissEmojiEffectUseCase(
    private val repository: EmojiEffectsRepository
) {
    operator fun invoke(id: String) {
        repository.dismissEffect(id)
    }

    fun dismissAll() {
        repository.cancelAllEffects()
    }
}
