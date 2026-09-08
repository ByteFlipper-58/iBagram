package org.telegram.messenger.feature.notifications.presentation

import org.telegram.messenger.feature.notifications.domain.model.BadgeCountModel
import org.telegram.messenger.feature.notifications.domain.model.BadgeSettingsModel
import org.telegram.messenger.feature.notifications.domain.model.NotificationSettingsModel

data class NotificationsUiState(
    val settings: NotificationSettingsModel = NotificationSettingsModel(),
    val badgeSettings: BadgeSettingsModel = BadgeSettingsModel(),
    val badgeCount: BadgeCountModel = BadgeCountModel(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
