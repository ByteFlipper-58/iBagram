package org.telegram.messenger.feature.stargifts.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.stargifts.domain.model.ProfileGiftsModel
import org.telegram.messenger.feature.stargifts.domain.repository.StarGiftsRepository

class ObserveProfileGiftsUseCase(
    private val repository: StarGiftsRepository
) {
    operator fun invoke(dialogId: Long): Flow<ProfileGiftsModel> {
        return repository.observeProfileGifts(dialogId)
    }
}
