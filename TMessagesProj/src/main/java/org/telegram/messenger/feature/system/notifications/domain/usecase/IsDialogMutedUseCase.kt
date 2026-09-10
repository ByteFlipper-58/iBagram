package org.telegram.messenger.feature.system.notifications.domain.usecase

import org.telegram.messenger.feature.system.notifications.domain.repository.NotificationsRepository

class IsDialogMutedUseCase(
    private val repository: NotificationsRepository
) {
    suspend operator fun invoke(dialogId: Long, topicId: Long = 0): Boolean =
        repository.isDialogMuted(dialogId, topicId)
}
