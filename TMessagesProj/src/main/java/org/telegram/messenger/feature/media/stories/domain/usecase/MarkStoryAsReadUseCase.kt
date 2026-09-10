package org.telegram.messenger.feature.media.stories.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.stories.domain.repository.StoriesRepository

class MarkStoryAsReadUseCase(
    private val repository: StoriesRepository
) {
    suspend operator fun invoke(dialogId: Long, storyId: Int): Result<Unit> =
        repository.markStoryAsRead(dialogId, storyId)
}
