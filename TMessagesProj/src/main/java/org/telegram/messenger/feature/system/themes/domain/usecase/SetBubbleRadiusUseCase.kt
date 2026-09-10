package org.telegram.messenger.feature.system.themes.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.themes.domain.repository.ThemeRepository

class SetBubbleRadiusUseCase(
    private val repository: ThemeRepository
) {
    suspend operator fun invoke(radius: Int): Result<Unit> {
        return repository.setBubbleRadius(radius)
    }
}
