package org.telegram.messenger.feature.media.stories.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.stories.domain.repository.StoriesRepository

class ToggleStoryPinUseCase(
    private val repository: StoriesRepository
) {
    suspend operator fun invoke(dialogId: Long, storyId: Int, pin: Boolean): Result<Unit> =
        repository.toggleStoryPin(dialogId, storyId, pin)
}
