package org.telegram.messenger.feature.stories.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.stories.domain.repository.StoriesRepository

class ActivateStealthModeUseCase(
    private val repository: StoriesRepository
) {
    suspend operator fun invoke(future: Boolean, past: Boolean): Result<Unit> =
        repository.activateStealthMode(future, past)
}
