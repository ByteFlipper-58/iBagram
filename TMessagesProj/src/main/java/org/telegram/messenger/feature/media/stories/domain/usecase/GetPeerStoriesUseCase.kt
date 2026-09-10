package org.telegram.messenger.feature.media.stories.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.stories.domain.model.StoryModel
import org.telegram.messenger.feature.media.stories.domain.repository.StoriesRepository

class GetPeerStoriesUseCase(
    private val repository: StoriesRepository
) {
    suspend operator fun invoke(dialogId: Long): Result<List<StoryModel>> =
        repository.getStories(dialogId)
}
