package org.telegram.messenger.feature.stories.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.stories.domain.model.StealthModeModel
import org.telegram.messenger.feature.stories.domain.repository.StoriesRepository

class ObserveStealthModeUseCase(
    private val repository: StoriesRepository
) {
    operator fun invoke(): Flow<StealthModeModel> = repository.observeStealthMode()
}
