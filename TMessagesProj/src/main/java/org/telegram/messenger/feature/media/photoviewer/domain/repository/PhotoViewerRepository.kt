package org.telegram.messenger.feature.media.photoviewer.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.media.photoviewer.domain.model.PhotoViewerMediaItem
import org.telegram.messenger.feature.media.photoviewer.domain.model.PhotoViewerState
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerActionType
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerEditMode
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerSelectType

/**
 * Domain repository contract managing the state and operations of the Photo/Video Viewer.
 */
interface PhotoViewerRepository {

    fun getState(): PhotoViewerState

    fun observeState(): StateFlow<PhotoViewerState>

    fun open(
        items: List<PhotoViewerMediaItem>,
        initialIndex: Int = 0,
        selectType: ViewerSelectType = ViewerSelectType.NO_SELECT
    )

    fun close()

    fun navigateTo(index: Int)

    fun next()

    fun previous()

    fun setEditMode(mode: ViewerEditMode)

    fun toggleActionBar()

    fun toggleCaptionExpanded()

    fun updateTransform(
        scale: Float,
        translationX: Float,
        translationY: Float,
        rotation: Float
    )

    fun resetTransform()

    fun updatePlayback(
        isPlaying: Boolean? = null,
        positionMs: Long? = null,
        durationMs: Long? = null,
        speed: Float? = null,
        quality: Int? = null,
        isMuted: Boolean? = null,
        isLooping: Boolean? = null,
        isBuffering: Boolean? = null
    )

    fun executeAction(action: ViewerActionType)
}
