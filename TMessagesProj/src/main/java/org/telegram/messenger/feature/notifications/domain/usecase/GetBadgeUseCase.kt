package org.telegram.messenger.feature.notifications.domain.usecase

import org.telegram.messenger.feature.notifications.domain.model.BadgeCountModel
import org.telegram.messenger.feature.notifications.domain.repository.NotificationsRepository

class GetBadgeUseCase(
    private val repository: NotificationsRepository
) {
    suspend operator fun invoke(): BadgeCountModel = repository.getBadge()
}
