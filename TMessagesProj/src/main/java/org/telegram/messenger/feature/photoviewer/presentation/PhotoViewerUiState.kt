package org.telegram.messenger.feature.photoviewer.presentation

import org.telegram.messenger.feature.photoviewer.data.mapper.PhotoViewerMapper
import org.telegram.messenger.feature.photoviewer.domain.model.PhotoViewerMediaItem
import org.telegram.messenger.feature.photoviewer.domain.model.PhotoViewerPlaybackState
import org.telegram.messenger.feature.photoviewer.domain.model.PhotoViewerState
import org.telegram.messenger.feature.photoviewer.domain.model.PhotoViewerTransform
import org.telegram.messenger.feature.photoviewer.domain.model.ViewerActionType
import org.telegram.messenger.feature.photoviewer.domain.model.ViewerEditMode
import org.telegram.messenger.feature.photoviewer.domain.model.ViewerSelectType

/**
 * UI State for PhotoViewer presentation layer.
 */
data class PhotoViewerUiState(
    val items: List<PhotoViewerMediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val selectType: ViewerSelectType = ViewerSelectType.NO_SELECT,
    val editMode: ViewerEditMode = ViewerEditMode.NONE,
    val isVisible: Boolean = false,
    val playbackState: PhotoViewerPlaybackState = PhotoViewerPlaybackState(),
    val transform: PhotoViewerTransform = PhotoViewerTransform(),
    val isActionBarVisible: Boolean = true,
    val isCaptionExpanded: Boolean = false,
    val availableActions: Set<ViewerActionType> = emptySet()
) {
    val currentItem: PhotoViewerMediaItem?
        get() = items.getOrNull(currentIndex)

    val indexIndicator: String
        get() = if (items.isNotEmpty()) "${currentIndex + 1} of ${items.size}" else ""

    val playbackTimeFormatted: String
        get() = PhotoViewerMapper.formatPlaybackTimeMs(playbackState.currentPositionMs)

    val durationFormatted: String
        get() = PhotoViewerMapper.formatPlaybackTimeMs(playbackState.durationMs)

    companion object {
        fun fromDomain(domainState: PhotoViewerState): PhotoViewerUiState {
            return PhotoViewerUiState(
                items = domainState.items,
                currentIndex = domainState.currentIndex,
                selectType = domainState.selectType,
                editMode = domainState.editMode,
                isVisible = domainState.isVisible,
                playbackState = domainState.playbackState,
                transform = domainState.transform,
                isActionBarVisible = domainState.isActionBarVisible,
                isCaptionExpanded = domainState.isCaptionExpanded,
                availableActions = domainState.availableActions
            )
        }
    }
}
