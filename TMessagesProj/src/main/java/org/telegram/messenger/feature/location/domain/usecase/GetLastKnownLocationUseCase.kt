package org.telegram.messenger.feature.location.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.location.domain.model.GeoPointModel
import org.telegram.messenger.feature.location.domain.repository.LocationRepository

class GetLastKnownLocationUseCase(
    private val repository: LocationRepository
) {
    suspend operator fun invoke(): Result<GeoPointModel?> {
        return repository.getLastKnownLocation()
    }
}
