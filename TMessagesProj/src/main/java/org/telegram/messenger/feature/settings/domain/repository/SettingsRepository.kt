package org.telegram.messenger.feature.settings.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.settings.domain.model.SettingsModel

/**
 * Clean domain repository contract for accessing and modifying settings.
 */
interface SettingsRepository {
    fun observeSettings(): Flow<SettingsModel>
    suspend fun getSettings(): SettingsModel
    suspend fun updateFontSize(size: Int): Result<Unit>
    suspend fun updateBubbleRadius(radius: Int): Result<Unit>
    suspend fun updateSaveToGallery(enabled: Boolean): Result<Unit>
    suspend fun updateStreamMedia(enabled: Boolean): Result<Unit>
    suspend fun updateSuggestStickers(enabled: Boolean): Result<Unit>
    suspend fun updateInappCamera(enabled: Boolean): Result<Unit>
    suspend fun updateDistanceSystemType(type: Int): Result<Unit>
    suspend fun updateSyncContacts(enabled: Boolean): Result<Unit>
    suspend fun updateSuggestContacts(enabled: Boolean): Result<Unit>
    suspend fun updateShowCallsTab(enabled: Boolean): Result<Unit>
}
