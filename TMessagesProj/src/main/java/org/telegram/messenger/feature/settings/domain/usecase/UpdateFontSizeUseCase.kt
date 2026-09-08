package org.telegram.messenger.feature.settings.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.settings.domain.repository.SettingsRepository

class UpdateFontSizeUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(fontSize: Int): Result<Unit> = repository.updateFontSize(fontSize)
}
