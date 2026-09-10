package org.telegram.messenger.feature.media.stories.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.stories.domain.model.StoryLimitModel
import org.telegram.messenger.feature.media.stories.domain.repository.StoriesRepository

class GetStoryLimitUseCase(
    private val repository: StoriesRepository
) {
    suspend operator fun invoke(): Result<StoryLimitModel> =
        repository.getStoryLimit()
}
