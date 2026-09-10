package org.telegram.messenger.feature.system.themes.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.themes.domain.repository.ThemeRepository

class SetThemeAccentUseCase(
    private val repository: ThemeRepository
) {
    suspend operator fun invoke(themeKey: String, accentId: Int): Result<Unit> {
        return repository.setThemeAccent(themeKey, accentId)
    }
}
