package org.telegram.messenger.feature.social.location.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.location.domain.model.GeoPointModel
import org.telegram.messenger.feature.social.location.domain.repository.LocationRepository

class GetLastKnownLocationUseCase(
    private val repository: LocationRepository
) {
    suspend operator fun invoke(): Result<GeoPointModel?> {
        return repository.getLastKnownLocation()
    }
}
