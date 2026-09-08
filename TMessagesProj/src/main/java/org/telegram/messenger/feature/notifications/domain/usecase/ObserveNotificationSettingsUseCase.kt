package org.telegram.messenger.feature.notifications.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.notifications.domain.model.NotificationSettingsModel
import org.telegram.messenger.feature.notifications.domain.repository.NotificationsRepository

class ObserveNotificationSettingsUseCase(
    private val repository: NotificationsRepository
) {
    operator fun invoke(): Flow<NotificationSettingsModel> = repository.observeSettings()
}
