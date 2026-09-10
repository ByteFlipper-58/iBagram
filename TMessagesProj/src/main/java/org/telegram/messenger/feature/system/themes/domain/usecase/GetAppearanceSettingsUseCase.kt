package org.telegram.messenger.feature.system.themes.domain.usecase

import org.telegram.messenger.feature.system.themes.domain.model.AppearanceSettingsModel
import org.telegram.messenger.feature.system.themes.domain.repository.ThemeRepository

class GetAppearanceSettingsUseCase(
    private val repository: ThemeRepository
) {
    operator fun invoke(): AppearanceSettingsModel {
        return repository.getAppearanceSettings()
    }
}
