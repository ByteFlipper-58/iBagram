package org.telegram.messenger.feature.stories.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.stories.domain.repository.StoriesRepository

class DeleteStoryUseCase(
    private val repository: StoriesRepository
) {
    suspend operator fun invoke(dialogId: Long, storyId: Int): Result<Unit> =
        repository.deleteStory(dialogId, storyId)
}
