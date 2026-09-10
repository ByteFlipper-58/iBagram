package org.telegram.messenger.feature.media.photoviewer.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.media.photoviewer.domain.model.PhotoViewerMediaItem
import org.telegram.messenger.feature.media.photoviewer.domain.model.PhotoViewerPlaybackState
import org.telegram.messenger.feature.media.photoviewer.domain.model.PhotoViewerState
import org.telegram.messenger.feature.media.photoviewer.domain.model.PhotoViewerTransform
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerActionType
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerEditMode
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerMediaType
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerSelectType
import org.telegram.messenger.feature.media.photoviewer.domain.repository.PhotoViewerRepository
import kotlin.math.roundToInt

/**
 * Validates and calculates boundary safe media indices during paging/swiping.
 */
class CalculateMediaPagingUseCase {

    data class PagingResult(
        val targetIndex: Int,
        val hasPrevious: Boolean,
        val hasNext: Boolean,
        val isValid: Boolean
    )

    operator fun invoke(currentIndex: Int, totalItems: Int, delta: Int): PagingResult {
        if (totalItems <= 0) {
            return PagingResult(
                targetIndex = 0,
                hasPrevious = false,
                hasNext = false,
                isValid = false
            )
        }
        val target = (currentIndex + delta).coerceIn(0, totalItems - 1)
        return PagingResult(
            targetIndex = target,
            hasPrevious = target > 0,
            hasNext = target < totalItems - 1,
            isValid = true
        )
    }
}

/**
 * Calculates gesture zoom and clamp transformations.
 * Automatically bounds zoom scale, resets translation on zoom 1.0, and snaps rotation to multiples of 90 degrees.
 */
class CalculateZoomTransformUseCase {

    operator fun invoke(
        currentTransform: PhotoViewerTransform,
        requestedScale: Float,
        requestedTranslationX: Float,
        requestedTranslationY: Float,
        requestedRotation: Float = currentTransform.rotation
    ): PhotoViewerTransform {
        val clampedScale = requestedScale.coerceIn(currentTransform.minScale, currentTransform.maxScale)

        // When not zoomed, translations should be centered (0.0f)
        val finalTranslationX = if (clampedScale <= 1.01f) 0.0f else requestedTranslationX
        val finalTranslationY = if (clampedScale <= 1.01f) 0.0f else requestedTranslationY

        // Normalize rotation to [0, 360)
        var normalizedRot = requestedRotation % 360f
        if (normalizedRot < 0f) normalizedRot += 360f

        return currentTransform.copy(
            scale = clampedScale,
            translationX = finalTranslationX,
            translationY = finalTranslationY,
            rotation = normalizedRot
        )
    }

    fun snapRotationQuarter(rotation: Float): Float {
        val steps = (rotation / 90f).roundToInt()
        var snapped = (steps * 90f) % 360f
        if (snapped < 0f) snapped += 360f
        return snapped
    }
}

/**
 * Determines which actions are valid based on media type, select mode, and editing state.
 */
class ValidateViewerActionsUseCase {

    operator fun invoke(
        mediaItem: PhotoViewerMediaItem?,
        selectType: ViewerSelectType,
        editMode: ViewerEditMode
    ): Set<ViewerActionType> {
        if (mediaItem == null) return emptySet()

        val actions = mutableSetOf<ViewerActionType>()

        if (editMode != ViewerEditMode.NONE) {
            // In editing mode, primary actions are rotated or saved
            actions.add(ViewerActionType.ROTATE)
            return actions
        }

        when (selectType) {
            ViewerSelectType.AVATAR -> {
                actions.add(ViewerActionType.SET_AVATAR)
                actions.add(ViewerActionType.EDIT)
                actions.add(ViewerActionType.ROTATE)
                return actions
            }
            ViewerSelectType.WALLPAPER,
            ViewerSelectType.STICKER,
            ViewerSelectType.GIF,
            ViewerSelectType.QR,
            ViewerSelectType.POLL_MEDIA,
            ViewerSelectType.POLL_MEDIA_EDIT -> {
                actions.add(ViewerActionType.SEND)
                actions.add(ViewerActionType.EDIT)
                return actions
            }
            ViewerSelectType.NO_SELECT -> {
                // Standard fullscreen viewer
                actions.add(ViewerActionType.FORWARD)
                actions.add(ViewerActionType.SHARE)
                actions.add(ViewerActionType.SAVE_TO_GALLERY)
                actions.add(ViewerActionType.DELETE)

                if (mediaItem.mediaType == ViewerMediaType.PHOTO || mediaItem.mediaType == ViewerMediaType.VIDEO) {
                    actions.add(ViewerActionType.EDIT)
                }

                if (mediaItem.mediaType == ViewerMediaType.PHOTO) {
                    actions.add(ViewerActionType.ROTATE)
                    actions.add(ViewerActionType.SET_AVATAR)
                }

                if (mediaItem.mediaType == ViewerMediaType.VIDEO || mediaItem.mediaType == ViewerMediaType.ROUND_VIDEO) {
                    actions.add(ViewerActionType.PIP)
                    actions.add(ViewerActionType.SPEED)
                    actions.add(ViewerActionType.QUALITY)
                }
            }
        }

        return actions
    }
}

/**
 * Resolves available video quality profiles and formats bitrate/label information.
 */
class ResolveMediaQualityUseCase {

    companion object {
        val SUPPORTED_QUALITIES = listOf(360, 480, 720, 1080, 1440, 2160)
    }

    data class QualityInfo(
        val quality: Int,
        val label: String,
        val estimatedBitrateKbps: Int
    )

    operator fun invoke(availableQualities: List<Int>, preferredQuality: Int): QualityInfo {
        val valid = if (availableQualities.isEmpty()) SUPPORTED_QUALITIES else availableQualities
        val chosen = valid.minByOrNull { kotlin.math.abs(it - preferredQuality) } ?: 720
        return QualityInfo(
            quality = chosen,
            label = "${chosen}p",
            estimatedBitrateKbps = estimateBitrate(chosen)
        )
    }

    private fun estimateBitrate(quality: Int): Int {
        return when {
            quality >= 2160 -> 15000
            quality >= 1440 -> 8000
            quality >= 1080 -> 4000
            quality >= 720 -> 2000
            quality >= 480 -> 1000
            else -> 600
        }
    }
}

class ObservePhotoViewerStateUseCase(
    private val repository: PhotoViewerRepository
) {
    operator fun invoke(): StateFlow<PhotoViewerState> = repository.observeState()
}

class GetPhotoViewerStateUseCase(
    private val repository: PhotoViewerRepository
) {
    operator fun invoke(): PhotoViewerState = repository.getState()
}

class OpenPhotoViewerUseCase(
    private val repository: PhotoViewerRepository
) {
    operator fun invoke(
        items: List<PhotoViewerMediaItem>,
        initialIndex: Int = 0,
        selectType: ViewerSelectType = ViewerSelectType.NO_SELECT
    ) {
        repository.open(items, initialIndex, selectType)
    }
}

class NavigatePhotoViewerUseCase(
    private val repository: PhotoViewerRepository
) {
    operator fun invoke(targetIndex: Int) {
        repository.navigateTo(targetIndex)
    }
}

class UpdatePlaybackStateUseCase(
    private val repository: PhotoViewerRepository
) {
    operator fun invoke(
        isPlaying: Boolean? = null,
        positionMs: Long? = null,
        durationMs: Long? = null,
        speed: Float? = null,
        quality: Int? = null,
        isMuted: Boolean? = null,
        isLooping: Boolean? = null,
        isBuffering: Boolean? = null
    ) {
        repository.updatePlayback(
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
}

class ClosePhotoViewerUseCase(
    private val repository: PhotoViewerRepository
) {
    operator fun invoke() {
        repository.close()
    }
}
