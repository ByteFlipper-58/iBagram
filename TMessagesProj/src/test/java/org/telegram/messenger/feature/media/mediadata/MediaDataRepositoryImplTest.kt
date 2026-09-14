package org.telegram.messenger.feature.media.mediadata

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.mediadata.data.datasource.MediaDataLocalDataSource
import org.telegram.messenger.feature.media.mediadata.data.datasource.MediaDataRemoteDataSource
import org.telegram.messenger.feature.media.mediadata.data.repository.MediaDataRepositoryImpl
import org.telegram.messenger.feature.media.mediadata.domain.model.MediaAlbumModel
import org.telegram.messenger.feature.media.mediadata.domain.model.MediaItemModel

class MediaDataRepositoryImplTest {

    private lateinit var localDataSource: MediaDataLocalDataSource
    private lateinit var remoteDataSource: MediaDataRemoteDataSource
    private lateinit var repository: MediaDataRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = MediaDataLocalDataSource(0)
        remoteDataSource = MediaDataRemoteDataSource(0)
        repository = MediaDataRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            dispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun getAlbums_returnsSuccessWithAlbums() = runBlocking {
        val item1 = MediaItemModel(id = 1, bucketId = 10, path = "/path/photo1.jpg")
        val album = MediaAlbumModel(id = 10, name = "Camera", coverPath = "/path/photo1.jpg", mediaCount = 1, items = listOf(item1))
        localDataSource.addAlbum(album)

        val result = repository.getAlbums()
        assertTrue(result is Result.Success)
        val albums = (result as Result.Success).data
        assertEquals(1, albums.size)
        assertEquals("Camera", albums[0].name)
    }

    @Test
    fun getMediaForAlbum_returnsItemsForRequestedAlbum() = runBlocking {
        val item1 = MediaItemModel(id = 1, bucketId = 10, path = "/path/photo1.jpg")
        val item2 = MediaItemModel(id = 2, bucketId = 10, path = "/path/photo2.jpg")
        val album = MediaAlbumModel(id = 10, name = "Camera", coverPath = "/path/photo1.jpg", mediaCount = 2, items = listOf(item1, item2))
        localDataSource.addAlbum(album)

        val result = repository.getMediaForAlbum(10)
        assertTrue(result is Result.Success)
        val items = (result as Result.Success).data
        assertEquals(2, items.size)
        assertEquals("/path/photo1.jpg", items[0].path)
        assertEquals("/path/photo2.jpg", items[1].path)
    }

    @Test
    fun getAllMedia_returnsCombinedMediaAcrossAlbums() = runBlocking {
        val item1 = MediaItemModel(id = 1, bucketId = 10, path = "/path/p1.jpg")
        val item2 = MediaItemModel(id = 2, bucketId = 20, path = "/path/p2.jpg")
        val album1 = MediaAlbumModel(id = 10, name = "Album 1", items = listOf(item1))
        val album2 = MediaAlbumModel(id = 20, name = "Album 2", items = listOf(item2))
        localDataSource.setAlbums(listOf(album1, album2))

        val result = repository.getAllMedia()
        assertTrue(result is Result.Success)
        val items = (result as Result.Success).data
        assertEquals(2, items.size)
    }

    @Test
    fun observeAlbums_emitsUpdates() = runBlocking {
        val album = MediaAlbumModel(id = 5, name = "Screenshots")
        localDataSource.addAlbum(album)

        val albums = repository.observeAlbums().first()
        assertEquals(1, albums.size)
        assertEquals("Screenshots", albums[0].name)
    }
}
