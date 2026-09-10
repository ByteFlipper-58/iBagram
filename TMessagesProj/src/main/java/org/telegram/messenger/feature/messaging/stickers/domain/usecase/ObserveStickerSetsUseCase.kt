package org.telegram.messenger.feature.messaging.stickers.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.stickers.domain.model.StickerSetModel
import org.telegram.messenger.feature.messaging.stickers.domain.repository.StickersRepository

class ObserveStickerSetsUseCase(
    private val repository: StickersRepository
) {
    operator fun invoke(type: Int = 0): Flow<List<StickerSetModel>> = repository.observeStickerSets(type)
}
