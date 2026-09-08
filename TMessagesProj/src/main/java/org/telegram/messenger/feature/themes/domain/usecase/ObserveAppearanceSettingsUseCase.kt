package org.telegram.messenger.feature.themes.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.themes.domain.model.AppearanceSettingsModel
import org.telegram.messenger.feature.themes.domain.repository.ThemeRepository

class ObserveAppearanceSettingsUseCase(
    private val repository: ThemeRepository
) {
    operator fun invoke(): Flow<AppearanceSettingsModel> {
        return repository.observeAppearanceSettings()
    }
}
