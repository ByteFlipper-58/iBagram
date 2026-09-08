package org.telegram.messenger.feature.settings.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.UserConfig
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.settings.data.mapper.SettingsMapper
import org.telegram.messenger.feature.settings.domain.model.SettingsModel
import org.telegram.messenger.feature.settings.domain.repository.SettingsRepository

/**
 * Adapter implementing [SettingsRepository] over legacy [SharedConfig] and [UserConfig].
 * Guarantees all read/write operations execute on the main thread and persist configs.
 */
class LegacySettingsRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : SettingsRepository {

    private val userConfig: UserConfig
        get() = UserConfig.getInstance(currentAccount)

    private val _settingsFlow = MutableStateFlow(loadSettingsSync())
    val settingsStateFlow = _settingsFlow.asStateFlow()

    private fun loadSettingsSync(): SettingsModel {
        return SettingsMapper.map(userConfig)
    }

    private fun refreshState() {
        _settingsFlow.value = loadSettingsSync()
    }

    override fun observeSettings(): Flow<SettingsModel> = callbackFlow {
        trySend(loadSettingsSync())

        val delegate = NotificationCenter.NotificationCenterDelegate { id, account, _ ->
            if (id == NotificationCenter.updateInterfaces ||
                id == NotificationCenter.mainUserInfoChanged ||
                id == NotificationCenter.notificationsSettingsUpdated
            ) {
                val updated = loadSettingsSync()
                _settingsFlow.value = updated
                trySend(updated)
            }
        }

        NotificationCenter.getInstance(currentAccount).addObserver(delegate, NotificationCenter.mainUserInfoChanged)
        NotificationCenter.getInstance(currentAccount).addObserver(delegate, NotificationCenter.notificationsSettingsUpdated)
        NotificationCenter.getGlobalInstance().addObserver(delegate, NotificationCenter.updateInterfaces)

        awaitClose {
            NotificationCenter.getInstance(currentAccount).removeObserver(delegate, NotificationCenter.mainUserInfoChanged)
            NotificationCenter.getInstance(currentAccount).removeObserver(delegate, NotificationCenter.notificationsSettingsUpdated)
            NotificationCenter.getGlobalInstance().removeObserver(delegate, NotificationCenter.updateInterfaces)
        }
    }

    override suspend fun getSettings(): SettingsModel = withContext(mainDispatcher) {
        val model = loadSettingsSync()
        _settingsFlow.value = model
        model
    }

    override suspend fun updateFontSize(size: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            SharedConfig.fontSize = size
            SharedConfig.saveConfig()
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, 0)
            refreshState()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update font size", e))
        }
    }

    override suspend fun updateBubbleRadius(radius: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            SharedConfig.bubbleRadius = radius
            SharedConfig.saveConfig()
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, 0)
            refreshState()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update bubble radius", e))
        }
    }

    override suspend fun updateSaveToGallery(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            SharedConfig.saveIncomingPhotos = enabled
            SharedConfig.saveConfig()
            refreshState()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update save to gallery", e))
        }
    }

    override suspend fun updateStreamMedia(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            SharedConfig.streamMedia = enabled
            SharedConfig.saveConfig()
            refreshState()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update stream media", e))
        }
    }

    override suspend fun updateSuggestStickers(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            SharedConfig.suggestStickers = if (enabled) 0 else 2
            SharedConfig.saveConfig()
            refreshState()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update suggest stickers", e))
        }
    }

    override suspend fun updateInappCamera(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            SharedConfig.inappCamera = enabled
            SharedConfig.saveConfig()
            refreshState()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update inapp camera", e))
        }
    }

    override suspend fun updateDistanceSystemType(type: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            SharedConfig.distanceSystemType = type
            SharedConfig.saveConfig()
            refreshState()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update distance system type", e))
        }
    }

    override suspend fun updateSyncContacts(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            userConfig.syncContacts = enabled
            userConfig.saveConfig(false)
            refreshState()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update sync contacts", e))
        }
    }

    override suspend fun updateSuggestContacts(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            userConfig.suggestContacts = enabled
            userConfig.saveConfig(false)
            refreshState()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update suggest contacts", e))
        }
    }

    override suspend fun updateShowCallsTab(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            userConfig.showCallsTab = enabled
            userConfig.saveConfig(false)
            refreshState()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update show calls tab", e))
        }
    }
}
