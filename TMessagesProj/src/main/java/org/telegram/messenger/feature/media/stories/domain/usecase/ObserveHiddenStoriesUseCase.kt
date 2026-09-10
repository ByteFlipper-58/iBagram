package org.telegram.messenger.feature.media.stories.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.stories.domain.model.PeerStoriesModel
import org.telegram.messenger.feature.media.stories.domain.repository.StoriesRepository

class ObserveHiddenStoriesUseCase(
    private val repository: StoriesRepository
) {
    operator fun invoke(): Flow<List<PeerStoriesModel>> = repository.observeHiddenStories()
}
