package org.telegram.messenger.feature.settings.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.settings.domain.model.SettingsModel
import org.telegram.messenger.feature.settings.domain.repository.SettingsRepository

class ObserveSettingsUseCase(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<SettingsModel> = repository.observeSettings()
}
