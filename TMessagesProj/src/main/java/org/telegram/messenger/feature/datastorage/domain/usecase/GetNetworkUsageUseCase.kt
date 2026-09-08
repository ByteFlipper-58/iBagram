package org.telegram.messenger.feature.datastorage.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.datastorage.domain.model.NetworkUsageModel
import org.telegram.messenger.feature.datastorage.domain.model.NetworkUsageType
import org.telegram.messenger.feature.datastorage.domain.repository.DataStorageRepository

class GetNetworkUsageUseCase(
    private val repository: DataStorageRepository
) {
    suspend operator fun invoke(type: NetworkUsageType): Result<NetworkUsageModel> =
        repository.getNetworkUsage(type)
}
