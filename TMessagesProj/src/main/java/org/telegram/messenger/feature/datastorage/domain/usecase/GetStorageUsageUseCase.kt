package org.telegram.messenger.feature.datastorage.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.datastorage.domain.model.StorageUsageModel
import org.telegram.messenger.feature.datastorage.domain.repository.DataStorageRepository

class GetStorageUsageUseCase(
    private val repository: DataStorageRepository
) {
    suspend operator fun invoke(): Result<StorageUsageModel> =
        repository.getStorageUsage()
}
