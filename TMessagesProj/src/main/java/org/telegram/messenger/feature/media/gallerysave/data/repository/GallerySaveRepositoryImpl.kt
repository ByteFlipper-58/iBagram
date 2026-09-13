package org.telegram.messenger.feature.media.gallerysave.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.media.gallerysave.data.datasource.GallerySaveLocalDataSource
import org.telegram.messenger.feature.media.gallerysave.data.datasource.GallerySaveRemoteDataSource
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveConfigModel
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveDialogExceptionModel
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveTargetSettingsModel
import org.telegram.messenger.feature.media.gallerysave.domain.repository.GallerySaveRepository

/**
 * Clean repository implementation coordinating local preferences and remote sync for Save-To-Gallery.
 */
class GallerySaveRepositoryImpl(
    private val account: Int = 0,
    private val remoteDataSource: GallerySaveRemoteDataSource = GallerySaveRemoteDataSource(account),
    private val localDataSource: GallerySaveLocalDataSource = GallerySaveLocalDataSource(account),
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : GallerySaveRepository {

    private val _configFlow = MutableStateFlow(localDataSource.readCurrentConfig())

    override fun observeConfig(): Flow<GallerySaveConfigModel> = _configFlow.asStateFlow()

    override fun getConfig(): GallerySaveConfigModel = _configFlow.value

    override fun getSettings(peerType: GallerySavePeerType): GallerySaveTargetSettingsModel {
        return _configFlow.value.getSettings(peerType)
    }

    override fun updateSettings(settings: GallerySaveTargetSettingsModel) {
        localDataSource.saveTargetSettings(settings)
        updateState()
    }

    override fun togglePeerType(peerType: GallerySavePeerType) {
        localDataSource.togglePeerType(peerType)
        updateState()
    }

    override fun setVideoLimit(peerType: GallerySavePeerType, limitBytes: Long) {
        localDataSource.setVideoLimit(peerType, limitBytes)
        updateState()
    }

    override fun getExceptions(peerType: GallerySavePeerType): List<GallerySaveDialogExceptionModel> {
        return localDataSource.readExceptions(peerType)
    }

    override fun setException(peerType: GallerySavePeerType, exception: GallerySaveDialogExceptionModel) {
        localDataSource.saveException(peerType, exception)
        updateState()
    }

    override fun removeException(peerType: GallerySavePeerType, dialogId: Long) {
        localDataSource.removeException(peerType, dialogId)
        updateState()
    }

    override fun removeAllExceptions(peerType: GallerySavePeerType) {
        localDataSource.removeAllExceptions(peerType)
        updateState()
    }

    private fun updateState() {
        _configFlow.value = localDataSource.readCurrentConfig()
    }
}
