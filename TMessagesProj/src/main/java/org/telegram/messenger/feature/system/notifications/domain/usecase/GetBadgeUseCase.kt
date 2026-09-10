package org.telegram.messenger.feature.system.notifications.domain.usecase

import org.telegram.messenger.feature.system.notifications.domain.model.BadgeCountModel
import org.telegram.messenger.feature.system.notifications.domain.repository.NotificationsRepository

class GetBadgeUseCase(
    private val repository: NotificationsRepository
) {
    suspend operator fun invoke(): BadgeCountModel = repository.getBadge()
}
