package org.telegram.messenger.feature.notifications.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.NotificationsController
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.notifications.data.mapper.NotificationMapper
import org.telegram.messenger.feature.notifications.domain.model.BadgeCountModel
import org.telegram.messenger.feature.notifications.domain.model.BadgeSettingsModel
import org.telegram.messenger.feature.notifications.domain.model.NotificationPeerType
import org.telegram.messenger.feature.notifications.domain.model.NotificationSettingsModel
import org.telegram.messenger.feature.notifications.domain.repository.NotificationsRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.tl.TL_account

class LegacyNotificationsRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : NotificationsRepository {

    private val notificationsController: NotificationsController
        get() = NotificationsController.getInstance(currentAccount)

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(currentAccount)

    private val connectionsManager: ConnectionsManager
        get() = ConnectionsManager.getInstance(currentAccount)

    private val messagesStorage: MessagesStorage
        get() = MessagesStorage.getInstance(currentAccount)

    private val preferences: SharedPreferences
        get() = MessagesController.getNotificationsSettings(currentAccount)

    override fun observeSettings(): Flow<NotificationSettingsModel> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.notificationsSettingsUpdated)
            .map { getSettings() }
            .onStart { emit(getSettings()) }
    }

    override suspend fun getSettings(): NotificationSettingsModel = withContext(mainDispatcher) {
        val currentTime = connectionsManager.currentTime
        NotificationMapper.mapSettings(preferences, notificationsController, currentTime)
    }

    override fun observeBadge(): Flow<BadgeCountModel> {
        return NotificationCenterFlowBridge.observeEvents(
            currentAccount,
            NotificationCenter.notificationsCountUpdated,
            NotificationCenter.updateInterfaces
        )
            .map { getBadge() }
            .onStart { emit(getBadge()) }
    }

    override suspend fun getBadge(): BadgeCountModel = withContext(mainDispatcher) {
        val totalUnread = notificationsController.totalAllUnreadCount
        val badgeCount = notificationsController.totalAllUnreadCount
        NotificationMapper.mapBadgeCount(totalUnread, badgeCount)
    }

    override fun observeBadgeSettings(): Flow<BadgeSettingsModel> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.notificationsSettingsUpdated)
            .map { getBadgeSettings() }
            .onStart { emit(getBadgeSettings()) }
    }

    override suspend fun getBadgeSettings(): BadgeSettingsModel = withContext(mainDispatcher) {
        NotificationMapper.mapBadgeSettings(preferences, notificationsController)
    }

    override suspend fun setPeerTypeNotificationsEnabled(
        type: NotificationPeerType,
        enabled: Boolean
    ): Result<Unit> = withContext(mainDispatcher) {
        try {
            when (type) {
                NotificationPeerType.STORIES -> {
                    preferences.edit().putBoolean("EnableAllStories", enabled).commit()
                    notificationsController.updateServerNotificationsSettings(NotificationsController.TYPE_PRIVATE)
                }
                NotificationPeerType.REACTIONS_MESSAGES -> {
                    preferences.edit().putBoolean("EnableReactionsMessages", enabled).commit()
                    notificationsController.updateServerNotificationsSettings(NotificationsController.TYPE_PRIVATE)
                }
                NotificationPeerType.REACTIONS_STORIES -> {
                    preferences.edit().putBoolean("EnableReactionsStories", enabled).commit()
                    notificationsController.updateServerNotificationsSettings(NotificationsController.TYPE_PRIVATE)
                }
                else -> {
                    val legacyType = when (type) {
                        NotificationPeerType.PRIVATE_CHATS -> NotificationsController.TYPE_PRIVATE
                        NotificationPeerType.GROUPS -> NotificationsController.TYPE_GROUP
                        NotificationPeerType.CHANNELS -> NotificationsController.TYPE_CHANNEL
                        else -> NotificationsController.TYPE_PRIVATE
                    }
                    val time = if (enabled) 0 else Int.MAX_VALUE
                    notificationsController.setGlobalNotificationsEnabled(legacyType, time)
                }
            }
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to update notification settings", e))
        }
    }

    override suspend fun setInChatSoundEnabled(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            preferences.edit().putBoolean("EnableInChatSound", enabled).commit()
            notificationsController.setInChatSoundEnabled(enabled)
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to toggle in-chat sound", e))
        }
    }

    override suspend fun setInAppSoundsEnabled(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            preferences.edit().putBoolean("EnableInAppSounds", enabled).commit()
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to toggle in-app sounds", e))
        }
    }

    override suspend fun setInAppVibrateEnabled(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            preferences.edit().putBoolean("EnableInAppVibrate", enabled).commit()
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to toggle in-app vibrate", e))
        }
    }

    override suspend fun setInAppPreviewEnabled(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            preferences.edit().putBoolean("EnableInAppPreview", enabled).commit()
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to toggle in-app preview", e))
        }
    }

    override suspend fun setContactJoinedNotificationsEnabled(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            preferences.edit().putBoolean("EnableContactJoined", enabled).commit()
            messagesController.enableJoined = enabled
            val req = TL_account.setContactSignUpNotification()
            req.silent = !enabled
            connectionsManager.sendRequest(req) { _, _ -> }
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to toggle contact joined notifications", e))
        }
    }

    override suspend fun setPinnedMessagesNotificationsEnabled(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            preferences.edit().putBoolean("PinnedMessages", enabled).commit()
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to toggle pinned messages notifications", e))
        }
    }

    override suspend fun updateBadgeSettings(settings: BadgeSettingsModel): Result<Unit> = withContext(mainDispatcher) {
        try {
            notificationsController.showBadgeNumber = settings.showBadgeNumber
            notificationsController.showBadgeMuted = settings.showBadgeMuted
            notificationsController.showBadgeMessages = settings.showBadgeMessages

            preferences.edit()
                .putBoolean("badgeNumber", settings.showBadgeNumber)
                .putBoolean("badgeNumberMuted", settings.showBadgeMuted)
                .putBoolean("badgeNumberMessages", settings.showBadgeMessages)
                .commit()

            notificationsController.updateBadge()
            messagesStorage.updateMutedDialogsFiltersCounters()
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to update badge settings", e))
        }
    }

    override suspend fun muteDialog(dialogId: Long, topicId: Long, mute: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            notificationsController.muteDialog(dialogId, topicId, mute)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to mute dialog", e))
        }
    }

    override suspend fun muteDialogUntil(dialogId: Long, topicId: Long, untilDate: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            notificationsController.muteUntil(dialogId, topicId, untilDate)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to mute dialog until date", e))
        }
    }

    override suspend fun isDialogMuted(dialogId: Long, topicId: Long): Boolean = withContext(mainDispatcher) {
        messagesController.isDialogMuted(dialogId, topicId)
    }

    override suspend fun refreshBadge(): Result<Unit> = withContext(mainDispatcher) {
        try {
            notificationsController.updateBadge()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to refresh badge", e))
        }
    }
}
