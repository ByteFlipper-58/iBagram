package org.telegram.messenger.feature.system.notifications.domain.usecase

import org.telegram.messenger.feature.system.notifications.domain.model.NotificationSettingsModel
import org.telegram.messenger.feature.system.notifications.domain.repository.NotificationsRepository

class GetNotificationSettingsUseCase(
    private val repository: NotificationsRepository
) {
    suspend operator fun invoke(): NotificationSettingsModel = repository.getSettings()
}
