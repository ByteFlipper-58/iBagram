package org.telegram.messenger.feature.messaging.bottomviews.domain.usecase

import org.telegram.messenger.feature.messaging.bottomviews.domain.repository.BottomViewsVisibilityRepository

class GetPriorityBottomContainerUseCase(
    private val repository: BottomViewsVisibilityRepository
) {
    operator fun invoke(): Int {
        return repository.getCurrentPriorityContainerId()
    }
}
