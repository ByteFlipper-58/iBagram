package org.telegram.messenger.feature.social.location.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.social.location.domain.model.GeoPointModel
import org.telegram.messenger.feature.social.location.domain.repository.LocationRepository

class ObserveLastKnownLocationUseCase(
    private val repository: LocationRepository
) {
    operator fun invoke(): Flow<GeoPointModel?> {
        return repository.observeLastKnownLocation()
    }
}
