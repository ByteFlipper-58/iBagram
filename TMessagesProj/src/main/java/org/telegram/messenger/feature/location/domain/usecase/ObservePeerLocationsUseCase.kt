package org.telegram.messenger.feature.location.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.location.domain.model.PeerLiveLocationModel
import org.telegram.messenger.feature.location.domain.repository.LocationRepository

class ObservePeerLocationsUseCase(
    private val repository: LocationRepository
) {
    operator fun invoke(dialogId: Long): Flow<List<PeerLiveLocationModel>> {
        return repository.observePeerLocations(dialogId)
    }
}
