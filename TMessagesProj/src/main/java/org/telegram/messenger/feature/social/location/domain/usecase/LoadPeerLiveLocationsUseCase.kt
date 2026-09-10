package org.telegram.messenger.feature.social.location.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.location.domain.model.PeerLiveLocationModel
import org.telegram.messenger.feature.social.location.domain.repository.LocationRepository

class LoadPeerLiveLocationsUseCase(
    private val repository: LocationRepository
) {
    suspend operator fun invoke(dialogId: Long): Result<List<PeerLiveLocationModel>> {
        return repository.loadPeerLiveLocations(dialogId)
    }
}
