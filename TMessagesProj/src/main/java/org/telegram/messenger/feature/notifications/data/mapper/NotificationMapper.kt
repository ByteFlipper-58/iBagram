package org.telegram.messenger.feature.notifications.data.mapper

import android.content.SharedPreferences
import org.telegram.messenger.NotificationsController
import org.telegram.messenger.feature.notifications.domain.model.BadgeCountModel
import org.telegram.messenger.feature.notifications.domain.model.BadgeSettingsModel
import org.telegram.messenger.feature.notifications.domain.model.NotificationSettingsModel

object NotificationMapper {

    fun mapSettings(
        preferences: SharedPreferences,
        controller: NotificationsController,
        currentTime: Int
    ): NotificationSettingsModel {
        val privateEnabled = preferences.getInt(
            NotificationsController.getGlobalNotificationsKey(NotificationsController.TYPE_PRIVATE), 0
        ) < currentTime

        val groupEnabled = preferences.getInt(
            NotificationsController.getGlobalNotificationsKey(NotificationsController.TYPE_GROUP), 0
        ) < currentTime

        val channelEnabled = preferences.getInt(
            NotificationsController.getGlobalNotificationsKey(NotificationsController.TYPE_CHANNEL), 0
        ) < currentTime

        return NotificationSettingsModel(
            privateChatsEnabled = privateEnabled,
            groupsEnabled = groupEnabled,
            channelsEnabled = channelEnabled,
            storiesEnabled = preferences.getBoolean("EnableAllStories", true),
            reactionsMessagesEnabled = preferences.getBoolean("EnableReactionsMessages", true),
            reactionsStoriesEnabled = preferences.getBoolean("EnableReactionsStories", true),
            inChatSoundEnabled = preferences.getBoolean("EnableInChatSound", true),
            inAppSoundsEnabled = preferences.getBoolean("EnableInAppSounds", true),
            inAppVibrateEnabled = preferences.getBoolean("EnableInAppVibrate", true),
            inAppPreviewEnabled = preferences.getBoolean("EnableInAppPreview", true),
            contactJoinedNotificationsEnabled = preferences.getBoolean("EnableContactJoined", true),
            pinnedMessagesNotificationsEnabled = preferences.getBoolean("PinnedMessages", true)
        )
    }

    fun mapBadgeSettings(
        preferences: SharedPreferences,
        controller: NotificationsController
    ): BadgeSettingsModel {
        return BadgeSettingsModel(
            showBadgeNumber = preferences.getBoolean("badgeNumber", controller.showBadgeNumber),
            showBadgeMuted = preferences.getBoolean("badgeNumberMuted", controller.showBadgeMuted),
            showBadgeMessages = preferences.getBoolean("badgeNumberMessages", controller.showBadgeMessages)
        )
    }

    fun mapBadgeCount(totalUnreadCount: Int, badgeCount: Int): BadgeCountModel {
        return BadgeCountModel(
            totalUnreadCount = totalUnreadCount,
            badgeCount = badgeCount
        )
    }
}
