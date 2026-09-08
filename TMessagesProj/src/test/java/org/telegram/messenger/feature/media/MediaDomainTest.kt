package org.telegram.messenger.feature.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.MediaController
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.data.mapper.MediaMapper
import org.telegram.messenger.feature.media.domain.model.MediaAlbumModel
import org.telegram.messenger.feature.media.domain.model.MediaItemModel
import org.telegram.messenger.feature.media.domain.repository.MediaRepository
import org.telegram.messenger.feature.media.domain.usecase.GetAlbumMediaUseCase
import org.telegram.messenger.feature.media.domain.usecase.GetAllMediaUseCase
import org.telegram.messenger.feature.media.domain.usecase.GetMediaAlbumsUseCase
import org.telegram.messenger.feature.media.domain.usecase.ObserveMediaAlbumsUseCase
import org.telegram.messenger.feature.media.presentation.MediaEvent
import org.telegram.messenger.feature.media.presentation.MediaUiState
import org.telegram.messenger.feature.media.presentation.MediaViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class MediaDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeMediaRepository : MediaRepository {
        val albumsFlow = MutableSharedFlow<List<MediaAlbumModel>>(replay = 1)
        var albumsList: List<MediaAlbumModel> = emptyList()
            set(value) {
                field = value
                albumsFlow.tryEmit(value)
            }
        var allMediaList: List<MediaItemModel> = emptyList()
        var shouldSucceed = true

        init {
            albumsFlow.tryEmit(albumsList)
        }

        override fun observeAlbums(): Flow<List<MediaAlbumModel>> = albumsFlow.asSharedFlow()

        override suspend fun getAlbums(): Result<List<MediaAlbumModel>> {
            return if (shouldSucceed) {
                Result.success(albumsList)
            } else {
                Result.failure(AppError.Generic("Failed to get albums"))
            }
        }

        override suspend fun getMediaForAlbum(albumId: Int): Result<List<MediaItemModel>> {
            return if (shouldSucceed) {
                val album = albumsList.firstOrNull { it.id == albumId }
                Result.success(album?.items ?: emptyList())
            } else {
                Result.failure(AppError.Generic("Failed to get media for album $albumId"))
            }
        }

        override suspend fun getAllMedia(): Result<List<MediaItemModel>> {
            return if (shouldSucceed) {
                Result.success(allMediaList)
            } else {
                Result.failure(AppError.Generic("Failed to get all media"))
            }
        }
    }

    @Test
    fun `MediaItemModel stores correct media properties`() {
        val item = MediaItemModel(
            id = 101,
            bucketId = 1,
            path = "/storage/emulated/0/DCIM/photo.jpg",
            isVideo = false,
            duration = 0,
            dateTaken = 1600000000L,
            width = 1920,
            height = 1080,
            size = 2048576L,
            hasSpoiler = false
        )
        assertEquals(101, item.id)
        assertEquals(1, item.bucketId)
        assertEquals("/storage/emulated/0/DCIM/photo.jpg", item.path)
        assertFalse(item.isVideo)
        assertEquals(1920, item.width)
        assertEquals(1080, item.height)
    }

    @Test
    fun `MediaAlbumModel holds album metadata and item list`() {
        val item1 = MediaItemModel(id = 1, bucketId = 10, path = "/path1.jpg")
        val item2 = MediaItemModel(id = 2, bucketId = 10, path = "/path2.mp4", isVideo = true, duration = 15)
        val album = MediaAlbumModel(
            id = 10,
            name = "Camera",
            coverPath = "/path1.jpg",
            mediaCount = 2,
            items = listOf(item1, item2)
        )
        assertEquals(10, album.id)
        assertEquals("Camera", album.name)
        assertEquals(2, album.items.size)
        assertTrue(album.items[1].isVideo)
        assertEquals(15, album.items[1].duration)
    }

    @Test
    fun `MediaMapper maps PhotoEntry into MediaItemModel`() {
        val entry = MediaController.PhotoEntry(
            1, // bucketId
            55, // imageId
            1650000000L, // dateTaken
            "/sdcard/DCIM/test.jpg", // path
            0, // orientation
            false, // isVideo
            1280, // width
            720, // height
            102400L // size
        )
        val mapped = MediaMapper.mapPhotoEntry(entry)
        assertTrue(mapped != null)
        assertEquals(55, mapped!!.id)
        assertEquals(1, mapped.bucketId)
        assertEquals("/sdcard/DCIM/test.jpg", mapped.path)
        assertFalse(mapped.isVideo)
        assertEquals(1280, mapped.width)
        assertEquals(720, mapped.height)
    }

    @Test
    fun `MediaMapper returns null for null inputs`() {
        assertNull(MediaMapper.mapPhotoEntry(null))
        assertNull(MediaMapper.mapAlbumEntry(null))
    }

    @Test
    fun `GetMediaAlbumsUseCase returns albums from repository`() = runTest {
        val repo = FakeMediaRepository()
        val album = MediaAlbumModel(id = 1, name = "Screenshots")
        repo.albumsList = listOf(album)

        val useCase = GetMediaAlbumsUseCase(repo)
        val result = useCase()
        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.size)
        assertEquals("Screenshots", result.data[0].name)
    }

    @Test
    fun `ObserveMediaAlbumsUseCase emits updates reactively`() = runTest {
        val repo = FakeMediaRepository()
        val useCase = ObserveMediaAlbumsUseCase(repo)

        val initial = useCase().first()
        assertTrue(initial.isEmpty())

        val album = MediaAlbumModel(id = 2, name = "Downloads")
        repo.albumsList = listOf(album)

        val updated = useCase().first()
        assertEquals(1, updated.size)
        assertEquals("Downloads", updated[0].name)
    }

    @Test
    fun `GetAlbumMediaUseCase returns items for requested album`() = runTest {
        val repo = FakeMediaRepository()
        val item = MediaItemModel(id = 7, bucketId = 5, path = "/file.png")
        val album = MediaAlbumModel(id = 5, name = "Saved", items = listOf(item))
        repo.albumsList = listOf(album)

        val useCase = GetAlbumMediaUseCase(repo)
        val result = useCase(5)
        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.size)
        assertEquals(7, result.data[0].id)

        repo.shouldSucceed = false
        val failResult = useCase(5)
        assertTrue(failResult is Result.Failure)
    }

    @Test
    fun `GetAllMediaUseCase returns all items from repository`() = runTest {
        val repo = FakeMediaRepository()
        val items = listOf(
            MediaItemModel(id = 1, bucketId = 1, path = "/a.jpg"),
            MediaItemModel(id = 2, bucketId = 2, path = "/b.mp4", isVideo = true)
        )
        repo.allMediaList = items

        val useCase = GetAllMediaUseCase(repo)
        val result = useCase()
        assertTrue(result is Result.Success)
        assertEquals(2, (result as Result.Success).data.size)

        repo.shouldSucceed = false
        val failResult = useCase()
        assertTrue(failResult is Result.Failure)
    }

    @Test
    fun `MediaViewModel initializes and selects first album by default`() = runTest {
        val repo = FakeMediaRepository()
        val item = MediaItemModel(id = 10, bucketId = 1, path = "/pic.jpg")
        val album = MediaAlbumModel(id = 1, name = "Camera", items = listOf(item))
        repo.albumsList = listOf(album)

        val viewModel = MediaViewModel(
            observeMediaAlbumsUseCase = ObserveMediaAlbumsUseCase(repo),
            getMediaAlbumsUseCase = GetMediaAlbumsUseCase(repo),
            getAlbumMediaUseCase = GetAlbumMediaUseCase(repo),
            getAllMediaUseCase = GetAllMediaUseCase(repo)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MediaUiState.Success)
        val successState = state as MediaUiState.Success
        assertEquals(1, successState.albums.size)
        assertEquals("Camera", successState.selectedAlbum?.name)
        assertEquals(1, successState.mediaItems.size)
        assertEquals(10, successState.mediaItems[0].id)
    }

    @Test
    fun `MediaViewModel selectAlbum updates selected album and emits event`() = runTest {
        val repo = FakeMediaRepository()
        val item1 = MediaItemModel(id = 1, bucketId = 1, path = "/pic1.jpg")
        val item2 = MediaItemModel(id = 2, bucketId = 2, path = "/pic2.jpg")
        val album1 = MediaAlbumModel(id = 1, name = "Camera", items = listOf(item1))
        val album2 = MediaAlbumModel(id = 2, name = "Screenshots", items = listOf(item2))
        repo.albumsList = listOf(album1, album2)

        val viewModel = MediaViewModel(
            observeMediaAlbumsUseCase = ObserveMediaAlbumsUseCase(repo),
            getMediaAlbumsUseCase = GetMediaAlbumsUseCase(repo),
            getAlbumMediaUseCase = GetAlbumMediaUseCase(repo),
            getAllMediaUseCase = GetAllMediaUseCase(repo)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val events = mutableListOf<MediaEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        viewModel.selectAlbum(2)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as MediaUiState.Success
        assertEquals(2, state.selectedAlbum?.id)
        assertEquals("Screenshots", state.selectedAlbum?.name)
        assertEquals(1, state.mediaItems.size)
        assertEquals(2, state.mediaItems[0].id)

        val selectedEvent = events.firstOrNull { it is MediaEvent.AlbumSelected }
        assertTrue(selectedEvent != null)
        assertEquals(2, (selectedEvent as MediaEvent.AlbumSelected).albumId)

        job.cancel()
    }

    @Test
    fun `MediaViewModel selectMedia emits MediaSelected event`() = runTest {
        val repo = FakeMediaRepository()
        val viewModel = MediaViewModel(
            observeMediaAlbumsUseCase = ObserveMediaAlbumsUseCase(repo),
            getMediaAlbumsUseCase = GetMediaAlbumsUseCase(repo),
            getAlbumMediaUseCase = GetAlbumMediaUseCase(repo),
            getAllMediaUseCase = GetAllMediaUseCase(repo)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val events = mutableListOf<MediaEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        viewModel.selectMedia(999)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = events.firstOrNull { it is MediaEvent.MediaSelected }
        assertTrue(event != null)
        assertEquals(999, (event as MediaEvent.MediaSelected).mediaId)

        job.cancel()
    }
}
