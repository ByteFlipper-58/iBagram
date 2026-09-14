package org.telegram.messenger.feature.system.settings.data.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.UserConfig
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.settings.data.mapper.SettingsMapper
import org.telegram.messenger.feature.system.settings.domain.model.SettingsModel

/**
 * Local data source managing client preferences, appearance settings,
 * and user configuration persistence.
 */
open class SettingsLocalDataSource(
    private val currentAccount: Int = 0,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private var isTestMode: Boolean = false

    private val _settingsFlow = MutableStateFlow(loadSettingsSafe())

    fun setTestMode(initialState: SettingsModel = SettingsModel()) {
        isTestMode = true
        _settingsFlow.value = initialState
    }

    private fun loadSettingsSafe(): SettingsModel {
        if (isTestMode) return SettingsModel()
        return try {
            val userConfig = UserConfig.getInstance(currentAccount)
            SettingsMapper.map(userConfig)
        } catch (_: Throwable) {
            SettingsModel()
        }
    }

    open fun observeSettings(): Flow<SettingsModel> = _settingsFlow.asStateFlow()

    open suspend fun getSettings(): SettingsModel = withContext(mainDispatcher) {
        if (!isTestMode) {
            _settingsFlow.value = loadSettingsSafe()
        }
        _settingsFlow.value
    }

    open suspend fun updateFontSize(size: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            _settingsFlow.value = _settingsFlow.value.copy(fontSize = size)
            if (!isTestMode) {
                SharedConfig.fontSize = size
                SharedConfig.saveConfig()
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, 0)
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update font size", e))
        }
    }

    open suspend fun updateBubbleRadius(radius: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            _settingsFlow.value = _settingsFlow.value.copy(bubbleRadius = radius)
            if (!isTestMode) {
                SharedConfig.bubbleRadius = radius
                SharedConfig.saveConfig()
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, 0)
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update bubble radius", e))
        }
    }

    open suspend fun updateSaveToGallery(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            _settingsFlow.value = _settingsFlow.value.copy(saveToGallery = enabled)
            if (!isTestMode) {
                SharedConfig.saveIncomingPhotos = enabled
                SharedConfig.saveConfig()
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update save to gallery", e))
        }
    }

    open suspend fun updateStreamMedia(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            _settingsFlow.value = _settingsFlow.value.copy(streamMedia = enabled)
            if (!isTestMode) {
                SharedConfig.streamMedia = enabled
                SharedConfig.saveConfig()
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update stream media", e))
        }
    }

    open suspend fun updateSuggestStickers(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            _settingsFlow.value = _settingsFlow.value.copy(suggestStickers = enabled)
            if (!isTestMode) {
                SharedConfig.suggestStickers = if (enabled) 0 else 2
                SharedConfig.saveConfig()
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update suggest stickers", e))
        }
    }

    open suspend fun updateInappCamera(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            _settingsFlow.value = _settingsFlow.value.copy(inappCamera = enabled)
            if (!isTestMode) {
                SharedConfig.inappCamera = enabled
                SharedConfig.saveConfig()
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update inapp camera", e))
        }
    }

    open suspend fun updateDistanceSystemType(type: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            _settingsFlow.value = _settingsFlow.value.copy(distanceSystemType = type)
            if (!isTestMode) {
                SharedConfig.distanceSystemType = type
                SharedConfig.saveConfig()
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update distance system type", e))
        }
    }

    open suspend fun updateSyncContacts(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            _settingsFlow.value = _settingsFlow.value.copy(syncContacts = enabled)
            if (!isTestMode) {
                val userConfig = UserConfig.getInstance(currentAccount)
                userConfig.syncContacts = enabled
                userConfig.saveConfig(false)
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update sync contacts", e))
        }
    }

    open suspend fun updateSuggestContacts(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            _settingsFlow.value = _settingsFlow.value.copy(suggestContacts = enabled)
            if (!isTestMode) {
                val userConfig = UserConfig.getInstance(currentAccount)
                userConfig.suggestContacts = enabled
                userConfig.saveConfig(false)
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update suggest contacts", e))
        }
    }

    open suspend fun updateShowCallsTab(enabled: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            _settingsFlow.value = _settingsFlow.value.copy(showCallsTab = enabled)
            if (!isTestMode) {
                val userConfig = UserConfig.getInstance(currentAccount)
                userConfig.showCallsTab = enabled
                userConfig.saveConfig(false)
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update show calls tab", e))
        }
    }
}
