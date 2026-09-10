package org.telegram.messenger.feature.system.settings.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.settings.domain.repository.SettingsRepository

class UpdateStreamMediaUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> = repository.updateStreamMedia(enabled)
}
