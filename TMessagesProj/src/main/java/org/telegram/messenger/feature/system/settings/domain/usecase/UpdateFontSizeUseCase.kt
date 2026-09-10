package org.telegram.messenger.feature.system.settings.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.settings.domain.repository.SettingsRepository

class UpdateFontSizeUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(fontSize: Int): Result<Unit> = repository.updateFontSize(fontSize)
}
