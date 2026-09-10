package org.telegram.messenger.feature.system.themes.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.themes.domain.repository.ThemeRepository

class ApplyThemeUseCase(
    private val repository: ThemeRepository
) {
    suspend operator fun invoke(themeKey: String, nightTheme: Boolean = false): Result<Unit> {
        return repository.applyTheme(themeKey, nightTheme)
    }
}
