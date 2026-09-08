package org.telegram.messenger.feature.datastorage.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.datastorage.domain.model.AutoDownloadNetworkType
import org.telegram.messenger.feature.datastorage.domain.model.AutoDownloadPresetModel
import org.telegram.messenger.feature.datastorage.domain.repository.DataStorageRepository

class GetAutoDownloadPresetUseCase(
    private val repository: DataStorageRepository
) {
    suspend operator fun invoke(type: AutoDownloadNetworkType): Result<AutoDownloadPresetModel> =
        repository.getAutoDownloadPreset(type)
}
