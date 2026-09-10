package org.telegram.messenger.feature.system.notifications.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.notifications.domain.repository.NotificationsRepository

class RefreshBadgeUseCase(
    private val repository: NotificationsRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.refreshBadge()
}
