package org.telegram.messenger.feature.media.photoviewer.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.media.photoviewer.data.datasource.PhotoViewerLocalDataSource
import org.telegram.messenger.feature.media.photoviewer.data.datasource.PhotoViewerRemoteDataSource
import org.telegram.messenger.feature.media.photoviewer.domain.model.PhotoViewerMediaItem
import org.telegram.messenger.feature.media.photoviewer.domain.model.PhotoViewerState
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerActionType
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerEditMode
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerSelectType
import org.telegram.messenger.feature.media.photoviewer.domain.repository.PhotoViewerRepository

class PhotoViewerRepositoryImpl(
    private val currentAccount: Int = 0,
    private val localDataSource: PhotoViewerLocalDataSource,
    private val remoteDataSource: PhotoViewerRemoteDataSource
) : PhotoViewerRepository {

    override fun getState(): PhotoViewerState = localDataSource.getState()

    override fun observeState(): StateFlow<PhotoViewerState> = localDataSource.state

    override fun open(
        items: List<PhotoViewerMediaItem>,
        initialIndex: Int,
        selectType: ViewerSelectType
    ) {
        localDataSource.open(items, initialIndex, selectType)
    }

    override fun close() {
        localDataSource.close()
    }

    override fun navigateTo(index: Int) {
        localDataSource.navigateTo(index)
    }

    override fun next() {
        localDataSource.next()
    }

    override fun previous() {
        localDataSource.previous()
    }

    override fun setEditMode(mode: ViewerEditMode) {
        localDataSource.setEditMode(mode)
    }

    override fun toggleActionBar() {
        localDataSource.toggleActionBar()
    }

    override fun toggleCaptionExpanded() {
        localDataSource.toggleCaptionExpanded()
    }

    override fun updateTransform(
        scale: Float,
        translationX: Float,
        translationY: Float,
        rotation: Float
    ) {
        localDataSource.updateTransform(scale, translationX, translationY, rotation)
    }

    override fun resetTransform() {
        localDataSource.resetTransform()
    }

    override fun updatePlayback(
        isPlaying: Boolean?,
        positionMs: Long?,
        durationMs: Long?,
        speed: Float?,
        quality: Int?,
        isMuted: Boolean?,
        isLooping: Boolean?,
        isBuffering: Boolean?
    ) {
        localDataSource.updatePlayback(
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            speed = speed,
            quality = quality,
            isMuted = isMuted,
            isLooping = isLooping,
            isBuffering = isBuffering
        )
    }

    override fun executeAction(action: ViewerActionType) {
        localDataSource.executeAction(action)
    }
}
