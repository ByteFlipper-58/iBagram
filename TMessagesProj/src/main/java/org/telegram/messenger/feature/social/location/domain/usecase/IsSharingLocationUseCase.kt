package org.telegram.messenger.feature.social.location.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.location.domain.repository.LocationRepository

class IsSharingLocationUseCase(
    private val repository: LocationRepository
) {
    suspend operator fun invoke(dialogId: Long): Result<Boolean> {
        return repository.isSharingLocation(dialogId)
    }
}
