package org.telegram.messenger.feature.notifications.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.notifications.domain.repository.NotificationsRepository

class ToggleContactJoinedNotificationsUseCase(
    private val repository: NotificationsRepository
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> =
        repository.setContactJoinedNotificationsEnabled(enabled)
}
