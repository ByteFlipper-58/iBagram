package org.telegram.messenger.feature.datastorage.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.datastorage.domain.model.KeepMediaSettingsModel
import org.telegram.messenger.feature.datastorage.domain.repository.DataStorageRepository

class GetKeepMediaSettingsUseCase(
    private val repository: DataStorageRepository
) {
    suspend operator fun invoke(): Result<KeepMediaSettingsModel> =
        repository.getKeepMediaSettings()
}
