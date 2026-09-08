package org.telegram.messenger.feature.stickers.domain.usecase

import org.telegram.messenger.feature.stickers.domain.model.StickerModel
import org.telegram.messenger.feature.stickers.domain.repository.StickersRepository

class GetStickersForEmojiUseCase(
    private val repository: StickersRepository
) {
    suspend operator fun invoke(emoji: String): List<StickerModel> = repository.getStickersForEmoji(emoji)
}
