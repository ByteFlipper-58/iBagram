package org.telegram.messenger.feature.themes.domain.usecase

import org.telegram.messenger.feature.themes.domain.model.ThemeModel
import org.telegram.messenger.feature.themes.domain.repository.ThemeRepository

class GetAvailableThemesUseCase(
    private val repository: ThemeRepository
) {
    operator fun invoke(): List<ThemeModel> {
        return repository.getAvailableThemes()
    }
}
