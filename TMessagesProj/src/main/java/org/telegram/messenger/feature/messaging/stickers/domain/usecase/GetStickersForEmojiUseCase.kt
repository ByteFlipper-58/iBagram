package org.telegram.messenger.feature.messaging.stickers.domain.usecase

import org.telegram.messenger.feature.messaging.stickers.domain.model.StickerModel
import org.telegram.messenger.feature.messaging.stickers.domain.repository.StickersRepository

class GetStickersForEmojiUseCase(
    private val repository: StickersRepository
) {
    suspend operator fun invoke(emoji: String): List<StickerModel> = repository.getStickersForEmoji(emoji)
}
