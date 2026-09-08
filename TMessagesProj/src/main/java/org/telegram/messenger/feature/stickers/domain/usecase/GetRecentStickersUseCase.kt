package org.telegram.messenger.feature.stickers.domain.usecase

import org.telegram.messenger.feature.stickers.domain.model.StickerModel
import org.telegram.messenger.feature.stickers.domain.repository.StickersRepository

class GetRecentStickersUseCase(
    private val repository: StickersRepository
) {
    suspend operator fun invoke(type: Int = 0): List<StickerModel> = repository.getRecentStickers(type)
}
