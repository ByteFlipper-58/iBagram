package org.telegram.messenger.feature.location.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.location.domain.repository.LocationRepository

class MarkLiveLocationsAsReadUseCase(
    private val repository: LocationRepository
) {
    suspend operator fun invoke(dialogId: Long): Result<Unit> {
        return repository.markLiveLocationsAsRead(dialogId)
    }
}
