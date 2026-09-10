package org.telegram.messenger.feature.system.themes.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.themes.domain.repository.ThemeRepository

class ResetAppearanceSettingsUseCase(
    private val repository: ThemeRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.resetToDefault()
    }
}
