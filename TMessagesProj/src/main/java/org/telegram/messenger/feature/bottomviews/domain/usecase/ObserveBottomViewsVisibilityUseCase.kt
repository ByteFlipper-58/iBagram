package org.telegram.messenger.feature.bottomviews.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.bottomviews.domain.model.BottomViewsVisibilityState
import org.telegram.messenger.feature.bottomviews.domain.repository.BottomViewsVisibilityRepository

class ObserveBottomViewsVisibilityUseCase(
    private val repository: BottomViewsVisibilityRepository
) {
    operator fun invoke(): Flow<BottomViewsVisibilityState> {
        return repository.observeState()
    }
}
