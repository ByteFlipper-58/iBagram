package org.telegram.messenger.feature.emojieffects.domain.usecase

import org.telegram.messenger.feature.emojieffects.domain.repository.EmojiEffectsRepository

class UpdateEmojiEffectProgressUseCase(
    private val repository: EmojiEffectsRepository
) {
    operator fun invoke(id: String, progress: Float) {
        if (progress >= 1f) {
            repository.removeEffect(id)
        } else {
            repository.updateEffectProgress(id, progress)
        }
    }
}
