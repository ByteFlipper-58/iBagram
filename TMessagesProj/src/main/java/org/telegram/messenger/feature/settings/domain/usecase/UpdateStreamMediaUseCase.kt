package org.telegram.messenger.feature.settings.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.settings.domain.repository.SettingsRepository

class UpdateStreamMediaUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> = repository.updateStreamMedia(enabled)
}
