package org.telegram.messenger.feature.datastorage.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.datastorage.domain.model.NetworkUsageModel
import org.telegram.messenger.feature.datastorage.domain.model.NetworkUsageType
import org.telegram.messenger.feature.datastorage.domain.repository.DataStorageRepository

class ObserveNetworkUsageUseCase(
    private val repository: DataStorageRepository
) {
    operator fun invoke(type: NetworkUsageType): Flow<NetworkUsageModel> =
        repository.observeNetworkUsage(type)
}
