package org.telegram.messenger.feature.notifications.domain.model

data class NotificationSettingsModel(
    val privateChatsEnabled: Boolean = true,
    val groupsEnabled: Boolean = true,
    val channelsEnabled: Boolean = true,
    val storiesEnabled: Boolean = true,
    val reactionsMessagesEnabled: Boolean = true,
    val reactionsStoriesEnabled: Boolean = true,
    val inChatSoundEnabled: Boolean = true,
    val inAppSoundsEnabled: Boolean = true,
    val inAppVibrateEnabled: Boolean = true,
    val inAppPreviewEnabled: Boolean = true,
    val contactJoinedNotificationsEnabled: Boolean = true,
    val pinnedMessagesNotificationsEnabled: Boolean = true
)
