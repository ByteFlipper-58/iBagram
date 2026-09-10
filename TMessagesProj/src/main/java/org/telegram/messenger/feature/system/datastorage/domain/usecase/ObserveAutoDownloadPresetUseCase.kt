package org.telegram.messenger.feature.system.datastorage.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.datastorage.domain.model.AutoDownloadNetworkType
import org.telegram.messenger.feature.system.datastorage.domain.model.AutoDownloadPresetModel
import org.telegram.messenger.feature.system.datastorage.domain.repository.DataStorageRepository

class ObserveAutoDownloadPresetUseCase(
    private val repository: DataStorageRepository
) {
    operator fun invoke(type: AutoDownloadNetworkType): Flow<AutoDownloadPresetModel> =
        repository.observeAutoDownloadPreset(type)
}
