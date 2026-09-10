package org.telegram.messenger.feature.social.location.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.location.domain.repository.LocationRepository

class SetProximityAlertUseCase(
    private val repository: LocationRepository
) {
    suspend operator fun invoke(dialogId: Long, distanceMeters: Int): Result<Unit> {
        return repository.setProximityAlert(dialogId, distanceMeters)
    }
}
