package org.telegram.messenger.feature.social.location.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.social.location.domain.model.LiveLocationSharingModel
import org.telegram.messenger.feature.social.location.domain.repository.LocationRepository

class ObserveActiveSharingsUseCase(
    private val repository: LocationRepository
) {
    operator fun invoke(): Flow<List<LiveLocationSharingModel>> {
        return repository.observeActiveSharings()
    }
}
