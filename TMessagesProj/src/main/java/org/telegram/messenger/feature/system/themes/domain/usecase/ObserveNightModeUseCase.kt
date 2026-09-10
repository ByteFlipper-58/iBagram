package org.telegram.messenger.feature.system.themes.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.feature.system.themes.domain.model.NightModeSettingsModel
import org.telegram.messenger.feature.system.themes.domain.repository.ThemeRepository

class ObserveNightModeUseCase(
    private val repository: ThemeRepository
) {
    operator fun invoke(): Flow<NightModeSettingsModel> {
        return repository.observeAppearanceSettings().map { it.nightModeSettings }
    }
}
