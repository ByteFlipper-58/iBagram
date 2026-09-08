package org.telegram.messenger.feature.settings.domain.usecase

import org.telegram.messenger.feature.settings.domain.model.SettingsModel
import org.telegram.messenger.feature.settings.domain.repository.SettingsRepository

class GetSettingsUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(): SettingsModel = repository.getSettings()
}
