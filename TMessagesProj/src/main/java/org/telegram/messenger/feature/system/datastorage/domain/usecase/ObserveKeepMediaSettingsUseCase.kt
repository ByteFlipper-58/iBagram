package org.telegram.messenger.feature.system.datastorage.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.datastorage.domain.model.KeepMediaSettingsModel
import org.telegram.messenger.feature.system.datastorage.domain.repository.DataStorageRepository

class ObserveKeepMediaSettingsUseCase(
    private val repository: DataStorageRepository
) {
    operator fun invoke(): Flow<KeepMediaSettingsModel> =
        repository.observeKeepMediaSettings()
}
