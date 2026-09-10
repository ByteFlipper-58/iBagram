package org.telegram.messenger.feature.business.stargifts.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftsCatalogModel
import org.telegram.messenger.feature.business.stargifts.domain.repository.StarGiftsRepository

class ObserveStarGiftsCatalogUseCase(
    private val repository: StarGiftsRepository
) {
    operator fun invoke(): Flow<StarGiftsCatalogModel> {
        return repository.observeCatalog()
    }
}
