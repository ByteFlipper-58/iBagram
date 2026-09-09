package org.telegram.messenger.feature.bottomviews.domain.usecase

import org.telegram.messenger.feature.bottomviews.domain.model.BottomViewsVisibilityState
import org.telegram.messenger.feature.bottomviews.domain.repository.BottomViewsVisibilityRepository

class GetBottomViewsStateUseCase(
    private val repository: BottomViewsVisibilityRepository
) {
    operator fun invoke(): BottomViewsVisibilityState {
        return repository.getState()
    }
}
