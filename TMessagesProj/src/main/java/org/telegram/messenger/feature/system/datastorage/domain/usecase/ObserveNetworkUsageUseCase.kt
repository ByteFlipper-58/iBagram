package org.telegram.messenger.feature.system.datastorage.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.datastorage.domain.model.NetworkUsageModel
import org.telegram.messenger.feature.system.datastorage.domain.model.NetworkUsageType
import org.telegram.messenger.feature.system.datastorage.domain.repository.DataStorageRepository

class ObserveNetworkUsageUseCase(
    private val repository: DataStorageRepository
) {
    operator fun invoke(type: NetworkUsageType): Flow<NetworkUsageModel> =
        repository.observeNetworkUsage(type)
}
