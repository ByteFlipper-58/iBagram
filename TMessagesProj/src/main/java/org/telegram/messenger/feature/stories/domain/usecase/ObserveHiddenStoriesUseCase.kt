package org.telegram.messenger.feature.stories.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.stories.domain.model.PeerStoriesModel
import org.telegram.messenger.feature.stories.domain.repository.StoriesRepository

class ObserveHiddenStoriesUseCase(
    private val repository: StoriesRepository
) {
    operator fun invoke(): Flow<List<PeerStoriesModel>> = repository.observeHiddenStories()
}
