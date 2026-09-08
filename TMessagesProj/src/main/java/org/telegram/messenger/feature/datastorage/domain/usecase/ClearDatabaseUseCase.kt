package org.telegram.messenger.feature.datastorage.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.datastorage.domain.repository.DataStorageRepository

class ClearDatabaseUseCase(
    private val repository: DataStorageRepository
) {
    suspend operator fun invoke(): Result<Unit> =
        repository.clearDatabase()
}
