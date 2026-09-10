package org.telegram.messenger.feature.emojieffects.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.emojieffects.domain.model.EmojiEffectsState
import org.telegram.messenger.feature.emojieffects.domain.repository.EmojiEffectsRepository

class ObserveEmojiEffectsStateUseCase(
    private val repository: EmojiEffectsRepository
) {
    operator fun invoke(): Flow<EmojiEffectsState> = repository.observeState()
}
