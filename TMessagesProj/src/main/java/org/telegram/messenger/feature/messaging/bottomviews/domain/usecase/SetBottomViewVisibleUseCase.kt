package org.telegram.messenger.feature.messaging.bottomviews.domain.usecase

import org.telegram.messenger.feature.messaging.bottomviews.domain.repository.BottomViewsVisibilityRepository

class SetBottomViewVisibleUseCase(
    private val repository: BottomViewsVisibilityRepository
) {
    operator fun invoke(containerId: Int, isVisible: Boolean, animated: Boolean = true) {
        repository.setViewVisible(containerId, isVisible, animated)
    }
}
