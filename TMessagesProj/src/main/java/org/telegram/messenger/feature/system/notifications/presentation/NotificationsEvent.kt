package org.telegram.messenger.feature.system.notifications.presentation

import org.telegram.messenger.feature.system.notifications.domain.model.BadgeSettingsModel
import org.telegram.messenger.feature.system.notifications.domain.model.NotificationPeerType

sealed interface NotificationsEvent {
    data class TogglePeerType(val type: NotificationPeerType, val enabled: Boolean) : NotificationsEvent
    data class ToggleInChatSound(val enabled: Boolean) : NotificationsEvent
    data class ToggleInAppSounds(val enabled: Boolean) : NotificationsEvent
    data class ToggleInAppVibrate(val enabled: Boolean) : NotificationsEvent
    data class ToggleInAppPreview(val enabled: Boolean) : NotificationsEvent
    data class ToggleContactJoined(val enabled: Boolean) : NotificationsEvent
    data class TogglePinnedMessages(val enabled: Boolean) : NotificationsEvent
    data class UpdateBadgeSettings(val settings: BadgeSettingsModel) : NotificationsEvent
    data class MuteDialog(val dialogId: Long, val topicId: Long = 0, val mute: Boolean) : NotificationsEvent
    data class MuteDialogUntil(val dialogId: Long, val topicId: Long = 0, val untilDate: Int) : NotificationsEvent
    object RefreshBadge : NotificationsEvent
    object ClearError : NotificationsEvent
}
