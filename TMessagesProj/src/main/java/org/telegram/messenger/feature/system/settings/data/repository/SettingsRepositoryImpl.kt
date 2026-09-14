package org.telegram.messenger.feature.system.settings.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.settings.data.datasource.SettingsLocalDataSource
import org.telegram.messenger.feature.system.settings.data.datasource.SettingsRemoteDataSource
import org.telegram.messenger.feature.system.settings.domain.model.SettingsModel
import org.telegram.messenger.feature.system.settings.domain.repository.SettingsRepository

/**
 * Implementation of [SettingsRepository] coordinating [SettingsLocalDataSource]
 * and [SettingsRemoteDataSource].
 */
class SettingsRepositoryImpl(
    private val localDataSource: SettingsLocalDataSource,
    private val remoteDataSource: SettingsRemoteDataSource
) : SettingsRepository {

    override fun observeSettings(): Flow<SettingsModel> {
        return localDataSource.observeSettings()
    }

    override suspend fun getSettings(): SettingsModel {
        return localDataSource.getSettings()
    }

    override suspend fun updateFontSize(size: Int): Result<Unit> {
        return localDataSource.updateFontSize(size)
    }

    override suspend fun updateBubbleRadius(radius: Int): Result<Unit> {
        return localDataSource.updateBubbleRadius(radius)
    }

    override suspend fun updateSaveToGallery(enabled: Boolean): Result<Unit> {
        return localDataSource.updateSaveToGallery(enabled)
    }

    override suspend fun updateStreamMedia(enabled: Boolean): Result<Unit> {
        return localDataSource.updateStreamMedia(enabled)
    }

    override suspend fun updateSuggestStickers(enabled: Boolean): Result<Unit> {
        return localDataSource.updateSuggestStickers(enabled)
    }

    override suspend fun updateInappCamera(enabled: Boolean): Result<Unit> {
        return localDataSource.updateInappCamera(enabled)
    }

    override suspend fun updateDistanceSystemType(type: Int): Result<Unit> {
        return localDataSource.updateDistanceSystemType(type)
    }

    override suspend fun updateSyncContacts(enabled: Boolean): Result<Unit> {
        return localDataSource.updateSyncContacts(enabled)
    }

    override suspend fun updateSuggestContacts(enabled: Boolean): Result<Unit> {
        return localDataSource.updateSuggestContacts(enabled)
    }

    override suspend fun updateShowCallsTab(enabled: Boolean): Result<Unit> {
        return localDataSource.updateShowCallsTab(enabled)
    }
}
