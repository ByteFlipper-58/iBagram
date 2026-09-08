package org.telegram.messenger.feature.notifications.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.notifications.domain.model.BadgeCountModel
import org.telegram.messenger.feature.notifications.domain.model.BadgeSettingsModel
import org.telegram.messenger.feature.notifications.domain.model.NotificationPeerType
import org.telegram.messenger.feature.notifications.domain.model.NotificationSettingsModel

interface NotificationsRepository {
    fun observeSettings(): Flow<NotificationSettingsModel>
    suspend fun getSettings(): NotificationSettingsModel

    fun observeBadge(): Flow<BadgeCountModel>
    suspend fun getBadge(): BadgeCountModel

    fun observeBadgeSettings(): Flow<BadgeSettingsModel>
    suspend fun getBadgeSettings(): BadgeSettingsModel

    suspend fun setPeerTypeNotificationsEnabled(type: NotificationPeerType, enabled: Boolean): Result<Unit>
    suspend fun setInChatSoundEnabled(enabled: Boolean): Result<Unit>
    suspend fun setInAppSoundsEnabled(enabled: Boolean): Result<Unit>
    suspend fun setInAppVibrateEnabled(enabled: Boolean): Result<Unit>
    suspend fun setInAppPreviewEnabled(enabled: Boolean): Result<Unit>
    suspend fun setContactJoinedNotificationsEnabled(enabled: Boolean): Result<Unit>
    suspend fun setPinnedMessagesNotificationsEnabled(enabled: Boolean): Result<Unit>

    suspend fun updateBadgeSettings(settings: BadgeSettingsModel): Result<Unit>

    suspend fun muteDialog(dialogId: Long, topicId: Long = 0, mute: Boolean): Result<Unit>
    suspend fun muteDialogUntil(dialogId: Long, topicId: Long = 0, untilDate: Int): Result<Unit>
    suspend fun isDialogMuted(dialogId: Long, topicId: Long = 0): Boolean

    suspend fun refreshBadge(): Result<Unit>
}
