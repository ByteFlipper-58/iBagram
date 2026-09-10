package org.telegram.messenger.feature.system.settings.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.settings.domain.model.SettingsModel
import org.telegram.messenger.feature.system.settings.domain.repository.SettingsRepository

class ObserveSettingsUseCase(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<SettingsModel> = repository.observeSettings()
}
