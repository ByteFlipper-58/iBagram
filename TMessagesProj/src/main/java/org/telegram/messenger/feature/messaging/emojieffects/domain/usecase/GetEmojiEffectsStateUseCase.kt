package org.telegram.messenger.feature.messaging.emojieffects.domain.usecase

import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiEffectsState
import org.telegram.messenger.feature.messaging.emojieffects.domain.repository.EmojiEffectsRepository

class GetEmojiEffectsStateUseCase(
    private val repository: EmojiEffectsRepository
) {
    operator fun invoke(): EmojiEffectsState = repository.getState()
}
