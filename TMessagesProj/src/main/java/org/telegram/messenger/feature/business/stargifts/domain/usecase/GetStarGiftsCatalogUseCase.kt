package org.telegram.messenger.feature.business.stargifts.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftModel
import org.telegram.messenger.feature.business.stargifts.domain.repository.StarGiftsRepository

class GetStarGiftsCatalogUseCase(
    private val repository: StarGiftsRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<List<StarGiftModel>> {
        return repository.getCatalog(forceRefresh)
    }
}
