package org.telegram.messenger.feature.stories.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.stories.domain.repository.StoriesRepository

class RefreshStoriesUseCase(
    private val repository: StoriesRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.refreshStories()
}
