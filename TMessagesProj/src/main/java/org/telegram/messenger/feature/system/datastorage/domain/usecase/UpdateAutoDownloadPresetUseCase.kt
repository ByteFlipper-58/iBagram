package org.telegram.messenger.feature.system.datastorage.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.datastorage.domain.model.AutoDownloadPresetModel
import org.telegram.messenger.feature.system.datastorage.domain.repository.DataStorageRepository

class UpdateAutoDownloadPresetUseCase(
    private val repository: DataStorageRepository
) {
    suspend operator fun invoke(preset: AutoDownloadPresetModel): Result<Unit> =
        repository.updateAutoDownloadPreset(preset)
}
