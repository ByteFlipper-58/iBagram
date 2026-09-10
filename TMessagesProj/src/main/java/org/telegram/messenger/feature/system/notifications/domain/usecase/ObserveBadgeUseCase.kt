package org.telegram.messenger.feature.system.notifications.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.notifications.domain.model.BadgeCountModel
import org.telegram.messenger.feature.system.notifications.domain.repository.NotificationsRepository

class ObserveBadgeUseCase(
    private val repository: NotificationsRepository
) {
    operator fun invoke(): Flow<BadgeCountModel> = repository.observeBadge()
}
