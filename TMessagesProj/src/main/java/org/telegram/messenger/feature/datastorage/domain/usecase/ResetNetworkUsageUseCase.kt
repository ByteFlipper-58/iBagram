package org.telegram.messenger.feature.datastorage.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.datastorage.domain.model.NetworkUsageType
import org.telegram.messenger.feature.datastorage.domain.repository.DataStorageRepository

class ResetNetworkUsageUseCase(
    private val repository: DataStorageRepository
) {
    suspend operator fun invoke(type: NetworkUsageType): Result<Unit> =
        repository.resetNetworkUsage(type)
}
