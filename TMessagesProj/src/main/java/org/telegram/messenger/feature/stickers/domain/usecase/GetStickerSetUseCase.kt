package org.telegram.messenger.feature.stickers.domain.usecase

import org.telegram.messenger.feature.stickers.domain.model.StickerSetModel
import org.telegram.messenger.feature.stickers.domain.repository.StickersRepository

class GetStickerSetUseCase(
    private val repository: StickersRepository
) {
    suspend operator fun invoke(id: Long): StickerSetModel? = repository.getStickerSet(id)
}
