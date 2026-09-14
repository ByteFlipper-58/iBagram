package org.telegram.messenger.feature.media.photoviewer.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.media.photoviewer.domain.model.PhotoViewerMediaItem
import org.telegram.messenger.feature.media.photoviewer.domain.model.PhotoViewerPlaybackState
import org.telegram.messenger.feature.media.photoviewer.domain.model.PhotoViewerState
import org.telegram.messenger.feature.media.photoviewer.domain.model.PhotoViewerTransform
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerActionType
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerEditMode
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerSelectType

class PhotoViewerLocalDataSource(
    private val currentAccount: Int = 0
) {
    private val _state = MutableStateFlow(PhotoViewerState())
    val state: StateFlow<PhotoViewerState> = _state.asStateFlow()
    private val lock = Any()

    fun getState(): PhotoViewerState {
        synchronized(lock) {
            return _state.value
        }
    }

    fun open(
        items: List<PhotoViewerMediaItem>,
        initialIndex: Int = 0,
        selectType: ViewerSelectType = ViewerSelectType.NO_SELECT
    ) {
        synchronized(lock) {
            val clampedIndex = if (items.isEmpty()) 0 else initialIndex.coerceIn(0, items.size - 1)
            val currentItem = items.getOrNull(clampedIndex)
            val actions = computeAvailableActions(currentItem, selectType, ViewerEditMode.NONE)

            val initialPlayback = if (currentItem?.durationSeconds != null && currentItem.durationSeconds > 0) {
                PhotoViewerPlaybackState(durationMs = currentItem.durationSeconds * 1000L)
            } else {
                PhotoViewerPlaybackState()
            }

            _state.value = PhotoViewerState(
                items = items,
                currentIndex = clampedIndex,
                selectType = selectType,
                editMode = ViewerEditMode.NONE,
                isVisible = true,
                playbackState = initialPlayback,
                transform = PhotoViewerTransform(),
                isActionBarVisible = true,
                isCaptionExpanded = false,
                availableActions = actions
            )
        }
    }

    fun close() {
        synchronized(lock) {
            _state.value = _state.value.copy(
                isVisible = false,
                playbackState = _state.value.playbackState.copy(isPlaying = false),
                editMode = ViewerEditMode.NONE
            )
        }
    }

    fun navigateTo(index: Int) {
        synchronized(lock) {
            val current = _state.value
            if (current.items.isEmpty()) return

            val targetIndex = index.coerceIn(0, current.items.size - 1)
            if (targetIndex == current.currentIndex) return

            val currentItem = current.items.getOrNull(targetIndex)
            val actions = computeAvailableActions(currentItem, current.selectType, current.editMode)
            val durationMs = if (currentItem?.durationSeconds != null && currentItem.durationSeconds > 0) {
                currentItem.durationSeconds * 1000L
            } else {
                0L
            }

            _state.value = current.copy(
                currentIndex = targetIndex,
                transform = PhotoViewerTransform(),
                playbackState = PhotoViewerPlaybackState(durationMs = durationMs),
                availableActions = actions
            )
        }
    }

    fun next() {
        synchronized(lock) {
            val current = _state.value
            if (current.currentIndex < current.items.size - 1) {
                navigateTo(current.currentIndex + 1)
            }
        }
    }

    fun previous() {
        synchronized(lock) {
            val current = _state.value
            if (current.currentIndex > 0) {
                navigateTo(current.currentIndex - 1)
            }
        }
    }

    fun setEditMode(mode: ViewerEditMode) {
        synchronized(lock) {
            val current = _state.value
            val actions = computeAvailableActions(current.currentItem, current.selectType, mode)
            _state.value = current.copy(
                editMode = mode,
                availableActions = actions
            )
        }
    }

    fun toggleActionBar() {
        synchronized(lock) {
            val current = _state.value
            _state.value = current.copy(isActionBarVisible = !current.isActionBarVisible)
        }
    }

    fun toggleCaptionExpanded() {
        synchronized(lock) {
            val current = _state.value
            _state.value = current.copy(isCaptionExpanded = !current.isCaptionExpanded)
        }
    }

    fun updateTransform(
        scale: Float,
        translationX: Float,
        translationY: Float,
        rotation: Float
    ) {
        synchronized(lock) {
            val current = _state.value
            val clampedScale = scale.coerceIn(1.0f, 3.0f)
            _state.value = current.copy(
                transform = current.transform.copy(
                    scale = clampedScale,
                    translationX = translationX,
                    translationY = translationY,
                    rotation = rotation
                )
            )
        }
    }

    fun resetTransform() {
        synchronized(lock) {
            val current = _state.value
            _state.value = current.copy(transform = PhotoViewerTransform())
        }
    }

    fun updatePlayback(
        isPlaying: Boolean? = null,
        positionMs: Long? = null,
        durationMs: Long? = null,
        speed: Float? = null,
        quality: Int? = null,
        isMuted: Boolean? = null,
        isLooping: Boolean? = null,
        isBuffering: Boolean? = null
    ) {
        synchronized(lock) {
            val current = _state.value
            val p = current.playbackState
            val updated = p.copy(
                isPlaying = isPlaying ?: p.isPlaying,
                currentPositionMs = positionMs ?: p.currentPositionMs,
                durationMs = durationMs ?: p.durationMs,
                playbackSpeed = speed ?: p.playbackSpeed,
                quality = quality ?: p.quality,
                isMuted = isMuted ?: p.isMuted,
                isLooping = isLooping ?: p.isLooping,
                isBuffering = isBuffering ?: p.isBuffering
            )
            _state.value = current.copy(playbackState = updated)
        }
    }

    fun executeAction(action: ViewerActionType) {
        synchronized(lock) {
            val current = _state.value
            when (action) {
                ViewerActionType.ROTATE -> {
                    val nextRot = (current.transform.rotation + 90f) % 360f
                    _state.value = current.copy(
                        transform = current.transform.copy(rotation = nextRot)
                    )
                }
                ViewerActionType.SPEED -> {
                    val nextSpeed = when (current.playbackState.playbackSpeed) {
                        1.0f -> 1.5f
                        1.5f -> 2.0f
                        2.0f -> 0.5f
                        else -> 1.0f
                    }
                    updatePlayback(speed = nextSpeed)
                }
                else -> {
                    // Handled upstream or by presentation
                }
            }
        }
    }

    private fun computeAvailableActions(
        item: PhotoViewerMediaItem?,
        selectType: ViewerSelectType,
        editMode: ViewerEditMode
    ): Set<ViewerActionType> {
        val actions = mutableSetOf<ViewerActionType>()
        if (item == null) return actions
        actions.add(ViewerActionType.SEND)
        actions.add(ViewerActionType.SHARE)
        actions.add(ViewerActionType.SAVE_TO_GALLERY)
        actions.add(ViewerActionType.ROTATE)
        actions.add(ViewerActionType.EDIT)
        return actions
    }
}
