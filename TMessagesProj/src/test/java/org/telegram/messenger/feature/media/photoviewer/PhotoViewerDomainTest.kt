package org.telegram.messenger.feature.media.photoviewer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.telegram.messenger.feature.media.photoviewer.data.mapper.PhotoViewerMapper
import org.telegram.messenger.feature.media.photoviewer.data.repository.LegacyPhotoViewerRepository
import org.telegram.messenger.feature.media.photoviewer.domain.model.PhotoViewerMediaItem
import org.telegram.messenger.feature.media.photoviewer.domain.model.PhotoViewerTransform
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerActionType
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerEditMode
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerMediaType
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerSelectType
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.CalculateMediaPagingUseCase
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.CalculateZoomTransformUseCase
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.ResolveMediaQualityUseCase
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.ValidateViewerActionsUseCase
import org.telegram.messenger.feature.media.photoviewer.presentation.PhotoViewerEvent
import org.telegram.messenger.feature.media.photoviewer.presentation.PhotoViewerViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoViewerDomainTest {

    @Test
    fun testMediaPagingCalculations() {
        val paging = CalculateMediaPagingUseCase()

        // Empty list
        val emptyResult = paging(currentIndex = 0, totalItems = 0, delta = 1)
        assertFalse(emptyResult.isValid)
        assertEquals(0, emptyResult.targetIndex)

        // Starting at index 0, total 5
        val nextResult = paging(currentIndex = 0, totalItems = 5, delta = 1)
        assertTrue(nextResult.isValid)
        assertEquals(1, nextResult.targetIndex)
        assertTrue(nextResult.hasPrevious)
        assertTrue(nextResult.hasNext)

        // Underflow attempt
        val underflowResult = paging(currentIndex = 0, totalItems = 5, delta = -1)
        assertTrue(underflowResult.isValid)
        assertEquals(0, underflowResult.targetIndex)
        assertFalse(underflowResult.hasPrevious)
        assertTrue(underflowResult.hasNext)

        // Reaching last element
        val lastResult = paging(currentIndex = 3, totalItems = 5, delta = 1)
        assertTrue(lastResult.isValid)
        assertEquals(4, lastResult.targetIndex)
        assertTrue(lastResult.hasPrevious)
        assertFalse(lastResult.hasNext)
    }

    @Test
    fun testZoomAndTransformCalculations() {
        val zoom = CalculateZoomTransformUseCase()
        val defaultTransform = PhotoViewerTransform(minScale = 1.0f, maxScale = 3.0f)

        // Clamp minimum zoom and reset translation
        val clampedMin = zoom(
            currentTransform = defaultTransform,
            requestedScale = 0.4f,
            requestedTranslationX = 120f,
            requestedTranslationY = 80f
        )
        assertEquals(1.0f, clampedMin.scale, 0.001f)
        assertEquals(0.0f, clampedMin.translationX, 0.001f)
        assertEquals(0.0f, clampedMin.translationY, 0.001f)

        // Clamp maximum zoom and preserve translation
        val clampedMax = zoom(
            currentTransform = defaultTransform,
            requestedScale = 5.0f,
            requestedTranslationX = 50f,
            requestedTranslationY = -30f
        )
        assertEquals(3.0f, clampedMax.scale, 0.001f)
        assertEquals(50.0f, clampedMax.translationX, 0.001f)
        assertEquals(-30.0f, clampedMax.translationY, 0.001f)

        // Snap rotation to 90-degree steps
        assertEquals(90f, zoom.snapRotationQuarter(85f), 0.001f)
        assertEquals(180f, zoom.snapRotationQuarter(182f), 0.001f)
        assertEquals(0f, zoom.snapRotationQuarter(358f), 0.001f)
        assertEquals(270f, zoom.snapRotationQuarter(-85f), 0.001f)
    }

    @Test
    fun testViewerActionValidationAndQuality() {
        val validator = ValidateViewerActionsUseCase()
        val qualityResolver = ResolveMediaQualityUseCase()

        val photoItem = PhotoViewerMediaItem(
            id = 1L,
            mediaType = ViewerMediaType.PHOTO
        )
        val videoItem = PhotoViewerMediaItem(
            id = 2L,
            mediaType = ViewerMediaType.VIDEO,
            durationSeconds = 120
        )

        // Standard Photo Actions
        val photoActions = validator(photoItem, ViewerSelectType.NO_SELECT, ViewerEditMode.NONE)
        assertTrue(photoActions.contains(ViewerActionType.FORWARD))
        assertTrue(photoActions.contains(ViewerActionType.SHARE))
        assertTrue(photoActions.contains(ViewerActionType.SAVE_TO_GALLERY))
        assertTrue(photoActions.contains(ViewerActionType.DELETE))
        assertTrue(photoActions.contains(ViewerActionType.ROTATE))
        assertTrue(photoActions.contains(ViewerActionType.SET_AVATAR))
        assertFalse(photoActions.contains(ViewerActionType.PIP))

        // Standard Video Actions
        val videoActions = validator(videoItem, ViewerSelectType.NO_SELECT, ViewerEditMode.NONE)
        assertTrue(videoActions.contains(ViewerActionType.PIP))
        assertTrue(videoActions.contains(ViewerActionType.SPEED))
        assertTrue(videoActions.contains(ViewerActionType.QUALITY))
        assertTrue(videoActions.contains(ViewerActionType.EDIT))

        // Avatar select mode
        val avatarActions = validator(photoItem, ViewerSelectType.AVATAR, ViewerEditMode.NONE)
        assertTrue(avatarActions.contains(ViewerActionType.SET_AVATAR))
        assertTrue(avatarActions.contains(ViewerActionType.EDIT))
        assertFalse(avatarActions.contains(ViewerActionType.FORWARD))

        // Edit mode (Crop)
        val editActions = validator(photoItem, ViewerSelectType.NO_SELECT, ViewerEditMode.CROP)
        assertEquals(setOf(ViewerActionType.ROTATE), editActions)

        // Video Quality Resolution
        val chosenQuality = qualityResolver(listOf(360, 720, 1080), preferredQuality = 800)
        assertEquals(720, chosenQuality.quality)
        assertEquals("720p", chosenQuality.label)
        assertEquals(2000, chosenQuality.estimatedBitrateKbps)
    }

    @Test
    fun testMapperAndFormatting() {
        // SelectType mappings
        assertEquals(ViewerSelectType.AVATAR, PhotoViewerMapper.mapLegacySelectType(1))
        assertEquals(ViewerSelectType.WALLPAPER, PhotoViewerMapper.mapLegacySelectType(3))
        assertEquals(ViewerSelectType.QR, PhotoViewerMapper.mapLegacySelectType(10))
        assertEquals(ViewerSelectType.STICKER, PhotoViewerMapper.mapLegacySelectType(11))
        assertEquals(ViewerSelectType.GIF, PhotoViewerMapper.mapLegacySelectType(12))
        assertEquals(ViewerSelectType.POLL_MEDIA, PhotoViewerMapper.mapLegacySelectType(13))
        assertEquals(ViewerSelectType.NO_SELECT, PhotoViewerMapper.mapLegacySelectType(-1))
        assertEquals(1, PhotoViewerMapper.toLegacySelectType(ViewerSelectType.AVATAR))

        // EditMode mappings
        assertEquals(ViewerEditMode.NONE, PhotoViewerMapper.mapLegacyEditMode(0))
        assertEquals(ViewerEditMode.CROP, PhotoViewerMapper.mapLegacyEditMode(1))
        assertEquals(ViewerEditMode.FILTER, PhotoViewerMapper.mapLegacyEditMode(2))
        assertEquals(ViewerEditMode.PAINT, PhotoViewerMapper.mapLegacyEditMode(3))
        assertEquals(ViewerEditMode.STICKER_MASK, PhotoViewerMapper.mapLegacyEditMode(4))
        assertEquals(ViewerEditMode.COVER, PhotoViewerMapper.mapLegacyEditMode(5))

        // Duration / Timecode formatting
        assertEquals("0:00", PhotoViewerMapper.formatDurationSeconds(0))
        assertEquals("1:05", PhotoViewerMapper.formatDurationSeconds(65))
        assertEquals("1:01:01", PhotoViewerMapper.formatDurationSeconds(3661))
        assertEquals("2:30", PhotoViewerMapper.formatPlaybackTimeMs(150000L))

        // Media type label
        assertEquals("Photo", PhotoViewerMapper.getMediaTypeLabel(ViewerMediaType.PHOTO))
        assertEquals("Video", PhotoViewerMapper.getMediaTypeLabel(ViewerMediaType.VIDEO))
        assertEquals("Video Message", PhotoViewerMapper.getMediaTypeLabel(ViewerMediaType.ROUND_VIDEO))
    }

    @Test
    fun testPhotoViewerViewModelMviLifecycle() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repository = LegacyPhotoViewerRepository()
        val viewModel = PhotoViewerViewModel(repository, scope = testScope)

        val item1 = PhotoViewerMediaItem(
            id = 100L,
            mediaType = ViewerMediaType.PHOTO,
            caption = "First photo"
        )
        val item2 = PhotoViewerMediaItem(
            id = 101L,
            mediaType = ViewerMediaType.VIDEO,
            durationSeconds = 60,
            caption = "Second video"
        )

        // 1. Initial State
        assertFalse(viewModel.uiState.value.isVisible)
        assertEquals(0, viewModel.uiState.value.items.size)

        // 2. Open Viewer
        viewModel.onEvent(PhotoViewerEvent.Open(items = listOf(item1, item2), initialIndex = 0))
        val openState = viewModel.uiState.value
        assertTrue(openState.isVisible)
        assertEquals(2, openState.items.size)
        assertEquals(0, openState.currentIndex)
        assertEquals("1 of 2", openState.indexIndicator)
        assertEquals("First photo", openState.currentItem?.caption)
        assertTrue(openState.availableActions.contains(ViewerActionType.ROTATE))

        // 3. Navigate Next
        viewModel.onEvent(PhotoViewerEvent.Next)
        val nextState = viewModel.uiState.value
        assertEquals(1, nextState.currentIndex)
        assertEquals("2 of 2", nextState.indexIndicator)
        assertEquals(ViewerMediaType.VIDEO, nextState.currentItem?.mediaType)
        assertTrue(nextState.availableActions.contains(ViewerActionType.PIP))

        // 4. Playback and Seek
        viewModel.onEvent(PhotoViewerEvent.TogglePlayback)
        assertTrue(viewModel.uiState.value.playbackState.isPlaying)

        viewModel.onEvent(PhotoViewerEvent.SeekTo(30000L))
        assertEquals(30000L, viewModel.uiState.value.playbackState.currentPositionMs)
        assertEquals("0:30", viewModel.uiState.value.playbackTimeFormatted)

        viewModel.onEvent(PhotoViewerEvent.SetSpeed(1.5f))
        assertEquals(1.5f, viewModel.uiState.value.playbackState.playbackSpeed, 0.001f)

        // 5. Enter Edit Mode
        viewModel.onEvent(PhotoViewerEvent.SetEditMode(ViewerEditMode.CROP))
        assertEquals(ViewerEditMode.CROP, viewModel.uiState.value.editMode)
        assertEquals(setOf(ViewerActionType.ROTATE), viewModel.uiState.value.availableActions)

        // 6. Close Viewer
        viewModel.onEvent(PhotoViewerEvent.Close)
        val closedState = viewModel.uiState.value
        assertFalse(closedState.isVisible)
        assertFalse(closedState.playbackState.isPlaying)
        assertEquals(ViewerEditMode.NONE, closedState.editMode)

        viewModel.clear()
    }
}
