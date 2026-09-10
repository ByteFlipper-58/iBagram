package org.telegram.messenger.feature.system.themes.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.themes.domain.model.ThemeModel
import org.telegram.messenger.feature.system.themes.domain.repository.ThemeRepository

class ObserveAvailableThemesUseCase(
    private val repository: ThemeRepository
) {
    operator fun invoke(): Flow<List<ThemeModel>> {
        return repository.observeAvailableThemes()
    }
}
