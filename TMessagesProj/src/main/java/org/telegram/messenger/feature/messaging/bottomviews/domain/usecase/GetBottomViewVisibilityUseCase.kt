package org.telegram.messenger.feature.messaging.bottomviews.domain.usecase

import org.telegram.messenger.feature.messaging.bottomviews.domain.repository.BottomViewsVisibilityRepository

class GetBottomViewVisibilityUseCase(
    private val repository: BottomViewsVisibilityRepository
) {
    operator fun invoke(containerId: Int): Float {
        return repository.getVisibility(containerId)
    }
}
