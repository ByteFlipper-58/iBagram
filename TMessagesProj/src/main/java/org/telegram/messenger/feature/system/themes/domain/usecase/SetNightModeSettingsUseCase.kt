package org.telegram.messenger.feature.system.themes.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.themes.domain.model.NightModeSettingsModel
import org.telegram.messenger.feature.system.themes.domain.repository.ThemeRepository

class SetNightModeSettingsUseCase(
    private val repository: ThemeRepository
) {
    suspend operator fun invoke(settings: NightModeSettingsModel): Result<Unit> {
        return repository.setNightModeSettings(settings)
    }
}
