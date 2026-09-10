package org.telegram.messenger.feature.system.notifications.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.notifications.domain.model.NotificationPeerType
import org.telegram.messenger.feature.system.notifications.domain.repository.NotificationsRepository

class TogglePeerNotificationsUseCase(
    private val repository: NotificationsRepository
) {
    suspend operator fun invoke(type: NotificationPeerType, enabled: Boolean): Result<Unit> =
        repository.setPeerTypeNotificationsEnabled(type, enabled)
}
