package org.telegram.messenger.feature.messaging.stickers.domain.usecase

import org.telegram.messenger.feature.messaging.stickers.domain.model.StickerSetModel
import org.telegram.messenger.feature.messaging.stickers.domain.repository.StickersRepository

class GetStickerSetsUseCase(
    private val repository: StickersRepository
) {
    suspend operator fun invoke(type: Int = 0): List<StickerSetModel> = repository.getStickerSets(type)
}
