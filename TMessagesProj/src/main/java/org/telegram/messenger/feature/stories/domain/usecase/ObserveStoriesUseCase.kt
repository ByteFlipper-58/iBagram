package org.telegram.messenger.feature.stories.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.stories.domain.model.PeerStoriesModel
import org.telegram.messenger.feature.stories.domain.repository.StoriesRepository

class ObserveStoriesUseCase(
    private val repository: StoriesRepository
) {
    operator fun invoke(): Flow<List<PeerStoriesModel>> = repository.observeStories()
}
