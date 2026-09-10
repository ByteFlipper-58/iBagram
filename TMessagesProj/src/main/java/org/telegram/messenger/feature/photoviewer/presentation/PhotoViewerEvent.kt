package org.telegram.messenger.feature.photoviewer.presentation

import org.telegram.messenger.feature.photoviewer.domain.model.PhotoViewerMediaItem
import org.telegram.messenger.feature.photoviewer.domain.model.ViewerActionType
import org.telegram.messenger.feature.photoviewer.domain.model.ViewerEditMode
import org.telegram.messenger.feature.photoviewer.domain.model.ViewerSelectType

/**
 * UI Events for PhotoViewer interactions.
 */
sealed class PhotoViewerEvent {

    data class Open(
        val items: List<PhotoViewerMediaItem>,
        val initialIndex: Int = 0,
        val selectType: ViewerSelectType = ViewerSelectType.NO_SELECT
    ) : PhotoViewerEvent()

    object Close : PhotoViewerEvent()

    object Next : PhotoViewerEvent()

    object Previous : PhotoViewerEvent()

    data class SelectIndex(val index: Int) : PhotoViewerEvent()

    data class SetEditMode(val mode: ViewerEditMode) : PhotoViewerEvent()

    object ToggleActionBar : PhotoViewerEvent()

    object ToggleCaption : PhotoViewerEvent()

    data class UpdateTransform(
        val scale: Float,
        val translationX: Float,
        val translationY: Float,
        val rotation: Float
    ) : PhotoViewerEvent()

    object ResetTransform : PhotoViewerEvent()

    object TogglePlayback : PhotoViewerEvent()

    data class SeekTo(val positionMs: Long) : PhotoViewerEvent()

    data class SetSpeed(val speed: Float) : PhotoViewerEvent()

    data class SetQuality(val quality: Int) : PhotoViewerEvent()

    data class ExecuteAction(val action: ViewerActionType) : PhotoViewerEvent()
}
