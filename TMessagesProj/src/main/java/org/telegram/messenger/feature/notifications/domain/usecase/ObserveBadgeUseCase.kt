package org.telegram.messenger.feature.notifications.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.notifications.domain.model.BadgeCountModel
import org.telegram.messenger.feature.notifications.domain.repository.NotificationsRepository

class ObserveBadgeUseCase(
    private val repository: NotificationsRepository
) {
    operator fun invoke(): Flow<BadgeCountModel> = repository.observeBadge()
}
