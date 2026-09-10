package org.telegram.messenger.feature.media.stories.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.stories.domain.repository.StoriesRepository

class ToggleStoryHiddenUseCase(
    private val repository: StoriesRepository
) {
    suspend operator fun invoke(dialogId: Long, hide: Boolean): Result<Unit> =
        repository.toggleStoryHidden(dialogId, hide)
}
