package org.telegram.messenger.feature.media.photoviewer

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.media.photoviewer.data.datasource.PhotoViewerLocalDataSource
import org.telegram.messenger.feature.media.photoviewer.data.datasource.PhotoViewerRemoteDataSource
import org.telegram.messenger.feature.media.photoviewer.data.repository.PhotoViewerRepositoryImpl
import org.telegram.messenger.feature.media.photoviewer.domain.model.PhotoViewerMediaItem
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerActionType
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerEditMode
import org.telegram.messenger.feature.media.photoviewer.domain.model.ViewerMediaType

class PhotoViewerRepositoryImplTest {

    private lateinit var localDataSource: PhotoViewerLocalDataSource
    private lateinit var remoteDataSource: PhotoViewerRemoteDataSource
    private lateinit var repository: PhotoViewerRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = PhotoViewerLocalDataSource(0)
        remoteDataSource = PhotoViewerRemoteDataSource(0)
        repository = PhotoViewerRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource
        )
    }

    @Test
    fun open_initializesStateCorrectly() = runBlocking {
        val item1 = PhotoViewerMediaItem(id = 1, path = "/path/1.jpg")
        val item2 = PhotoViewerMediaItem(id = 2, path = "/path/2.jpg")
        repository.open(listOf(item1, item2), initialIndex = 1)

        val state = repository.observeState().first()
        assertTrue(state.isVisible)
        assertEquals(2, state.totalCount)
        assertEquals(1, state.currentIndex)
        assertEquals(item2, state.currentItem)
        assertTrue(state.hasPrevious)
        assertFalse(state.hasNext)
    }

    @Test
    fun navigate_previous_and_next_updatesIndex() = runBlocking {
        val items = (1..3).map { PhotoViewerMediaItem(id = it.toLong(), path = "/path/$it.jpg") }
        repository.open(items, initialIndex = 1)

        repository.previous()
        assertEquals(0, repository.getState().currentIndex)

        repository.next()
        assertEquals(1, repository.getState().currentIndex)

        repository.next()
        assertEquals(2, repository.getState().currentIndex)
    }

    @Test
    fun close_resetsVisibilityAndEditMode() {
        val items = listOf(PhotoViewerMediaItem(id = 1))
        repository.open(items)
        repository.setEditMode(ViewerEditMode.CROP)
        assertTrue(repository.getState().isVisible)
        assertEquals(ViewerEditMode.CROP, repository.getState().editMode)

        repository.close()
        assertFalse(repository.getState().isVisible)
        assertEquals(ViewerEditMode.NONE, repository.getState().editMode)
    }

    @Test
    fun updatePlayback_updatesStateProperties() {
        val items = listOf(PhotoViewerMediaItem(id = 1, mediaType = ViewerMediaType.VIDEO, durationSeconds = 30))
        repository.open(items)

        repository.updatePlayback(isPlaying = true, positionMs = 5000L, speed = 1.5f)
        val playback = repository.getState().playbackState
        assertTrue(playback.isPlaying)
        assertEquals(5000L, playback.currentPositionMs)
        assertEquals(1.5f, playback.playbackSpeed, 0.01f)
    }

    @Test
    fun updateTransform_and_reset_worksCorrectly() {
        repository.open(listOf(PhotoViewerMediaItem(id = 1)))
        repository.updateTransform(scale = 2.0f, translationX = 10f, translationY = 20f, rotation = 90f)

        val t = repository.getState().transform
        assertEquals(2.0f, t.scale, 0.01f)
        assertEquals(10f, t.translationX, 0.01f)
        assertEquals(20f, t.translationY, 0.01f)
        assertEquals(90f, t.rotation, 0.01f)

        repository.resetTransform()
        assertEquals(1.0f, repository.getState().transform.scale, 0.01f)
    }

    @Test
    fun executeAction_rotate_and_speed_updatesState() {
        repository.open(listOf(PhotoViewerMediaItem(id = 1)))
        repository.executeAction(ViewerActionType.ROTATE)
        assertEquals(90f, repository.getState().transform.rotation, 0.01f)

        repository.executeAction(ViewerActionType.SPEED)
        assertEquals(1.5f, repository.getState().playbackState.playbackSpeed, 0.01f)
    }
}
