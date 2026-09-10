package org.telegram.messenger.feature.emojieffects.domain.usecase

import org.telegram.messenger.feature.emojieffects.domain.model.EmojiEffectsState
import org.telegram.messenger.feature.emojieffects.domain.repository.EmojiEffectsRepository

class GetEmojiEffectsStateUseCase(
    private val repository: EmojiEffectsRepository
) {
    operator fun invoke(): EmojiEffectsState = repository.getState()
}
