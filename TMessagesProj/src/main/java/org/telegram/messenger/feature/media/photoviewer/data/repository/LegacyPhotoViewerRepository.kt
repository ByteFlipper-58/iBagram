package org.telegram.messenger.feature.media.photoviewer.data.repository

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
import org.telegram.messenger.feature.media.photoviewer.domain.repository.PhotoViewerRepository
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.CalculateMediaPagingUseCase
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.CalculateZoomTransformUseCase
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.ValidateViewerActionsUseCase

/**
 * Thread-safe StateFlow implementation of PhotoViewerRepository adapting legacy PhotoViewer state.
 */
class LegacyPhotoViewerRepository(
    private val pagingUseCase: CalculateMediaPagingUseCase = CalculateMediaPagingUseCase(),
    private val zoomUseCase: CalculateZoomTransformUseCase = CalculateZoomTransformUseCase(),
    private val actionsUseCase: ValidateViewerActionsUseCase = ValidateViewerActionsUseCase()
) : PhotoViewerRepository {

    private val _state = MutableStateFlow(PhotoViewerState())
    private val lock = Any()

    override fun getState(): PhotoViewerState {
        synchronized(lock) {
            return _state.value
        }
    }

    override fun observeState(): StateFlow<PhotoViewerState> {
        return _state.asStateFlow()
    }

    override fun open(
        items: List<PhotoViewerMediaItem>,
        initialIndex: Int,
        selectType: ViewerSelectType
    ) {
        synchronized(lock) {
            val clampedIndex = if (items.isEmpty()) 0 else initialIndex.coerceIn(0, items.size - 1)
            val currentItem = items.getOrNull(clampedIndex)
            val actions = actionsUseCase(currentItem, selectType, ViewerEditMode.NONE)

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

    override fun close() {
        synchronized(lock) {
            _state.value = _state.value.copy(
                isVisible = false,
                playbackState = _state.value.playbackState.copy(isPlaying = false),
                editMode = ViewerEditMode.NONE
            )
        }
    }

    override fun navigateTo(index: Int) {
        synchronized(lock) {
            val current = _state.value
            if (current.items.isEmpty()) return

            val targetIndex = index.coerceIn(0, current.items.size - 1)
            if (targetIndex == current.currentIndex) return

            val currentItem = current.items.getOrNull(targetIndex)
            val actions = actionsUseCase(currentItem, current.selectType, current.editMode)
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

    override fun next() {
        synchronized(lock) {
            val current = _state.value
            val paging = pagingUseCase(current.currentIndex, current.items.size, 1)
            if (paging.isValid) {
                navigateTo(paging.targetIndex)
            }
        }
    }

    override fun previous() {
        synchronized(lock) {
            val current = _state.value
            val paging = pagingUseCase(current.currentIndex, current.items.size, -1)
            if (paging.isValid) {
                navigateTo(paging.targetIndex)
            }
        }
    }

    override fun setEditMode(mode: ViewerEditMode) {
        synchronized(lock) {
            val current = _state.value
            val actions = actionsUseCase(current.currentItem, current.selectType, mode)
            _state.value = current.copy(
                editMode = mode,
                availableActions = actions
            )
        }
    }

    override fun toggleActionBar() {
        synchronized(lock) {
            val current = _state.value
            _state.value = current.copy(isActionBarVisible = !current.isActionBarVisible)
        }
    }

    override fun toggleCaptionExpanded() {
        synchronized(lock) {
            val current = _state.value
            _state.value = current.copy(isCaptionExpanded = !current.isCaptionExpanded)
        }
    }

    override fun updateTransform(
        scale: Float,
        translationX: Float,
        translationY: Float,
        rotation: Float
    ) {
        synchronized(lock) {
            val current = _state.value
            val newTransform = zoomUseCase(
                currentTransform = current.transform,
                requestedScale = scale,
                requestedTranslationX = translationX,
                requestedTranslationY = translationY,
                requestedRotation = rotation
            )
            _state.value = current.copy(transform = newTransform)
        }
    }

    override fun resetTransform() {
        synchronized(lock) {
            val current = _state.value
            _state.value = current.copy(transform = PhotoViewerTransform())
        }
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

    override fun executeAction(action: ViewerActionType) {
        synchronized(lock) {
            val current = _state.value
            when (action) {
                ViewerActionType.ROTATE -> {
                    val nextRot = zoomUseCase.snapRotationQuarter(current.transform.rotation + 90f)
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
                ViewerActionType.PIP -> {
                    // Handled upstream/presentation delegate
                }
                else -> {
                    // Handled upstream
                }
            }
        }
    }
}
