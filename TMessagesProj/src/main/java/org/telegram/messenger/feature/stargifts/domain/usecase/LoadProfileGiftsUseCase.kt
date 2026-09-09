package org.telegram.messenger.feature.stargifts.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.stargifts.domain.model.ProfileGiftsModel
import org.telegram.messenger.feature.stargifts.domain.model.StarGiftFilter
import org.telegram.messenger.feature.stargifts.domain.repository.StarGiftsRepository

class LoadProfileGiftsUseCase(
    private val repository: StarGiftsRepository
) {
    suspend operator fun invoke(
        dialogId: Long,
        offset: String? = null,
        limit: Int = 30,
        filter: StarGiftFilter = StarGiftFilter()
    ): Result<ProfileGiftsModel> {
        return repository.loadProfileGifts(dialogId, offset, limit, filter)
    }
}
