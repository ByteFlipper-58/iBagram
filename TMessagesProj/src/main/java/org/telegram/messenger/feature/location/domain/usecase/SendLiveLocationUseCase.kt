package org.telegram.messenger.feature.location.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.location.domain.repository.LocationRepository

class SendLiveLocationUseCase(
    private val repository: LocationRepository
) {
    suspend operator fun invoke(
        dialogId: Long,
        latitude: Double,
        longitude: Double,
        periodSeconds: Int,
        proximityRadiusMeters: Int = 0
    ): Result<Unit> {
        return repository.sendLiveLocation(dialogId, latitude, longitude, periodSeconds, proximityRadiusMeters)
    }
}
