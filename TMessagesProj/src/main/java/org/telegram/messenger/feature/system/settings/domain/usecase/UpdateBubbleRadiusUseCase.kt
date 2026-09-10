package org.telegram.messenger.feature.system.settings.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.settings.domain.repository.SettingsRepository

class UpdateBubbleRadiusUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(bubbleRadius: Int): Result<Unit> = repository.updateBubbleRadius(bubbleRadius)
}
