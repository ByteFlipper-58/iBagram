package org.telegram.messenger.feature.system.notifications.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.notifications.domain.repository.NotificationsRepository

class TogglePinnedMessagesNotificationsUseCase(
    private val repository: NotificationsRepository
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> =
        repository.setPinnedMessagesNotificationsEnabled(enabled)
}
