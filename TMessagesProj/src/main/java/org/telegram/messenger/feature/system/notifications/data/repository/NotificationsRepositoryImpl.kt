package org.telegram.messenger.feature.system.notifications.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.NotificationsController
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.notifications.data.datasource.NotificationsLocalDataSource
import org.telegram.messenger.feature.system.notifications.data.datasource.NotificationsRemoteDataSource
import org.telegram.messenger.feature.system.notifications.data.mapper.NotificationMapper
import org.telegram.messenger.feature.system.notifications.domain.model.BadgeCountModel
import org.telegram.messenger.feature.system.notifications.domain.model.BadgeSettingsModel
import org.telegram.messenger.feature.system.notifications.domain.model.NotificationPeerType
import org.telegram.messenger.feature.system.notifications.domain.model.NotificationSettingsModel
import org.telegram.messenger.feature.system.notifications.domain.repository.NotificationsRepository

/**
 * Clean repository implementation coordinating local and remote data sources for notification operations,
 * incrementally strangling and displacing monolithic legacy logic from NotificationsController.
 */
class NotificationsRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: NotificationsLocalDataSource,
    private val remoteDataSource: NotificationsRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : NotificationsRepository {

    private fun postNotificationSafely(eventId: Int, vararg args: Any?) {
        try {
            NotificationCenter.getInstance(currentAccount)?.postNotificationName(eventId, *args)
        } catch (_: Throwable) {
            // Ignored when running outside Android runtime in JVM unit tests
        }
    }

    override fun observeSettings(): Flow<NotificationSettingsModel> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.notificationsSettingsUpdated)
            .map { getSettings() }
            .onStart { emit(getSettings()) }
            .flowOn(mainDispatcher)
    }

    companion object {
        const val TYPE_GROUP = 0
        const val TYPE_PRIVATE = 1
        const val TYPE_CHANNEL = 2
        const val TYPE_STORIES = 3
        const val TYPE_REACTIONS_MESSAGES = 4
        const val TYPE_REACTIONS_STORIES = 5

        const val KEY_PRIVATE = "EnableAll2"
        const val KEY_GROUP = "EnableGroup2"
        const val KEY_CHANNEL = "EnableChannel2"
    }

    override suspend fun getSettings(): NotificationSettingsModel = withContext(mainDispatcher) {
        val currentTime = localDataSource.getCurrentTime()
        val privateEnabled = localDataSource.getInt(KEY_PRIVATE, 0) < currentTime
        val groupEnabled = localDataSource.getInt(KEY_GROUP, 0) < currentTime
        val channelEnabled = localDataSource.getInt(KEY_CHANNEL, 0) < currentTime

        NotificationSettingsModel(
            privateChatsEnabled = privateEnabled,
            groupsEnabled = groupEnabled,
            channelsEnabled = channelEnabled,
            storiesEnabled = localDataSource.getBoolean("EnableAllStories", true),
            reactionsMessagesEnabled = localDataSource.getBoolean("EnableReactionsMessages", true),
            reactionsStoriesEnabled = localDataSource.getBoolean("EnableReactionsStories", true),
            inChatSoundEnabled = localDataSource.getBoolean("EnableInChatSound", true),
            inAppSoundsEnabled = localDataSource.getBoolean("EnableInAppSounds", true),
            inAppVibrateEnabled = localDataSource.getBoolean("EnableInAppVibrate", true),
            inAppPreviewEnabled = localDataSource.getBoolean("EnableInAppPreview", true),
            contactJoinedNotificationsEnabled = localDataSource.getBoolean("EnableContactJoined", true),
            pinnedMessagesNotificationsEnabled = localDataSource.getBoolean("PinnedMessages", true)
        )
    }

    override fun observeBadge(): Flow<BadgeCountModel> {
        return NotificationCenterFlowBridge.observeEvents(
            currentAccount,
            NotificationCenter.notificationsCountUpdated,
            NotificationCenter.updateInterfaces
        )
            .map { getBadge() }
            .onStart { emit(getBadge()) }
            .flowOn(mainDispatcher)
    }

    override suspend fun getBadge(): BadgeCountModel = withContext(mainDispatcher) {
        val count = localDataSource.getTotalUnreadCount()
        NotificationMapper.mapBadgeCount(count, count)
    }

    override fun observeBadgeSettings(): Flow<BadgeSettingsModel> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.notificationsSettingsUpdated)
            .map { getBadgeSettings() }
            .onStart { emit(getBadgeSettings()) }
            .flowOn(mainDispatcher)
    }

    override suspend fun getBadgeSettings(): BadgeSettingsModel = withContext(mainDispatcher) {
        BadgeSettingsModel(
            showBadgeNumber = localDataSource.getShowBadgeNumber(),
            showBadgeMuted = localDataSource.getShowBadgeMuted(),
            showBadgeMessages = localDataSource.getShowBadgeMessages()
        )
    }

    override suspend fun setPeerTypeNotificationsEnabled(
        type: NotificationPeerType,
        enabled: Boolean
    ): Result<Unit> = withContext(mainDispatcher) {
        try {
            when (type) {
                NotificationPeerType.STORIES -> {
                    localDataSource.putBoolean("EnableAllStories", enabled)
                    try {
                        NotificationsController.getInstance(currentAccount)?.updateServerNotificationsSettings(TYPE_PRIVATE)
                    } catch (_: Throwable) {}
                }
                NotificationPeerType.REACTIONS_MESSAGES -> {
                    localDataSource.putBoolean("EnableReactionsMessages", enabled)
                    try {
                        NotificationsController.getInstance(currentAccount)?.updateServerNotificationsSettings(TYPE_PRIVATE)
                    } catch (_: Throwable) {}
                }
                NotificationPeerType.REACTIONS_STORIES -> {
                    localDataSource.putBoolean("EnableReactionsStories", enabled)
                    try {
                        NotificationsController.getInstance(currentAccount)?.updateServerNotificationsSettings(TYPE_PRIVATE)
                    } catch (_: Throwable) {}
                }
                else -> {
                    val legacyType = when (type) {
                        NotificationPeerType.PRIVATE_CHATS -> TYPE_PRIVATE
                        NotificationPeerType.GROUPS -> TYPE_GROUP
                        NotificationPeerType.CHANNELS -> TYPE_CHANNEL
                        else -> TYPE_PRIVATE
                    }
                    val time = if (enabled) 0 else Int.MAX_VALUE
                    localDataSource.setGlobalNotificationsEnabled(legacyType, time)
                }
            }
            postNotificationSafely(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to update peer notification settings", e))
        }
    }

    override suspend fun setInChatSoundEnabled(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.setInChatSoundEnabled(enabled)
            postNotificationSafely(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to toggle in-chat sound", e))
        }
    }

    override suspend fun setInAppSoundsEnabled(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.putBoolean("EnableInAppSounds", enabled)
            postNotificationSafely(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to toggle in-app sounds", e))
        }
    }

    override suspend fun setInAppVibrateEnabled(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.putBoolean("EnableInAppVibrate", enabled)
            postNotificationSafely(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to toggle in-app vibrate", e))
        }
    }

    override suspend fun setInAppPreviewEnabled(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.putBoolean("EnableInAppPreview", enabled)
            postNotificationSafely(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to toggle in-app preview", e))
        }
    }

    override suspend fun setContactJoinedNotificationsEnabled(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.putBoolean("EnableContactJoined", enabled)
            try {
                MessagesController.getInstance(currentAccount)?.enableJoined = enabled
            } catch (_: Throwable) {}
            remoteDataSource.setContactSignUpNotification(silent = !enabled)
            postNotificationSafely(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to toggle contact joined notifications", e))
        }
    }

    override suspend fun setPinnedMessagesNotificationsEnabled(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.putBoolean("PinnedMessages", enabled)
            postNotificationSafely(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to toggle pinned messages notifications", e))
        }
    }

    override suspend fun updateBadgeSettings(settings: BadgeSettingsModel): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.setBadgeSettings(
                showNumber = settings.showBadgeNumber,
                showMuted = settings.showBadgeMuted,
                showMessages = settings.showBadgeMessages
            )
            localDataSource.updateBadge()
            localDataSource.updateMutedDialogsFiltersCounters()
            postNotificationSafely(NotificationCenter.notificationsSettingsUpdated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to update badge settings", e))
        }
    }

    override suspend fun muteDialog(dialogId: Long, topicId: Long, mute: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.muteDialog(dialogId, topicId, mute)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to mute dialog", e))
        }
    }

    override suspend fun muteDialogUntil(dialogId: Long, topicId: Long, untilDate: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.muteDialogUntil(dialogId, topicId, untilDate)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to mute dialog until date", e))
        }
    }

    override suspend fun isDialogMuted(dialogId: Long, topicId: Long): Boolean = withContext(mainDispatcher) {
        localDataSource.isDialogMuted(dialogId, topicId)
    }

    override suspend fun refreshBadge(): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.updateBadge()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to refresh badge", e))
        }
    }
}
