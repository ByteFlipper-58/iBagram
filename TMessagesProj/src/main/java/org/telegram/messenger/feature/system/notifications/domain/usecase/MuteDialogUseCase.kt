package org.telegram.messenger.feature.system.notifications.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.notifications.domain.repository.NotificationsRepository

class MuteDialogUseCase(
    private val repository: NotificationsRepository
) {
    suspend operator fun invoke(dialogId: Long, topicId: Long = 0, mute: Boolean): Result<Unit> =
        repository.muteDialog(dialogId, topicId, mute)

    suspend fun until(dialogId: Long, topicId: Long = 0, untilDate: Int): Result<Unit> =
        repository.muteDialogUntil(dialogId, topicId, untilDate)
}
