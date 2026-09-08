package org.telegram.messenger.feature.datastorage.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.datastorage.domain.model.StorageUsageModel
import org.telegram.messenger.feature.datastorage.domain.repository.DataStorageRepository

class ObserveStorageUsageUseCase(
    private val repository: DataStorageRepository
) {
    operator fun invoke(): Flow<StorageUsageModel> =
        repository.observeStorageUsage()
}
