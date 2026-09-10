package org.telegram.messenger.feature.system.settings.domain.usecase

import org.telegram.messenger.feature.system.settings.domain.model.SettingsModel
import org.telegram.messenger.feature.system.settings.domain.repository.SettingsRepository

class GetSettingsUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(): SettingsModel = repository.getSettings()
}
