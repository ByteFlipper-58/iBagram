package org.telegram.messenger.feature.messaging.stickers

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.stickers.data.datasource.StickersLocalDataSource
import org.telegram.messenger.feature.messaging.stickers.data.datasource.StickersRemoteDataSource
import org.telegram.messenger.feature.messaging.stickers.data.repository.StickersRepositoryImpl
import org.telegram.messenger.feature.messaging.stickers.domain.model.StickerModel
import org.telegram.messenger.feature.messaging.stickers.domain.model.StickerSetModel

class StickersRepositoryImplTest {

    private lateinit var localDataSource: StickersLocalDataSource
    private lateinit var remoteDataSource: StickersRemoteDataSource
    private lateinit var repository: StickersRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = StickersLocalDataSource(0)
        remoteDataSource = StickersRemoteDataSource(0)
        repository = StickersRepositoryImpl(0, localDataSource, remoteDataSource)
    }

    @Test
    fun testInitialStickerSetsEmpty() = runBlocking {
        val sets = repository.getStickerSets(0)
        assertNotNull(sets)
    }

    @Test
    fun testSetAndGetStickerSets() = runBlocking {
        val set = StickerSetModel(
            id = 123L,
            accessHash = 456L,
            title = "Cool Stickers",
            shortName = "cool_stickers",
            count = 10,
            isInstalled = true
        )
        localDataSource.setStickerSets(0, listOf(set))

        val sets = repository.getStickerSets(0)
        assertEquals(1, sets.size)
        assertEquals(123L, sets[0].id)
        assertEquals("Cool Stickers", sets[0].title)

        val observed = repository.observeStickerSets(0).first()
        assertEquals(1, observed.size)
        assertEquals(123L, observed[0].id)
    }

    @Test
    fun testGetStickerSetById() = runBlocking {
        val set = StickerSetModel(
            id = 999L,
            accessHash = 888L,
            title = "Test Pack",
            shortName = "test_pack",
            count = 5
        )
        localDataSource.setStickerSets(0, listOf(set))

        val found = repository.getStickerSet(999L)
        assertNotNull(found)
        assertEquals("Test Pack", found?.title)

        val notFound = repository.getStickerSet(111L)
        assertNull(notFound)
    }

    @Test
    fun testRecentStickers() = runBlocking {
        val sticker = StickerModel(
            id = 1L,
            accessHash = 200L,
            mimeType = "image/webp"
        )
        localDataSource.setRecentStickers(0, listOf(sticker))

        val recent = repository.getRecentStickers(0)
        assertEquals(1, recent.size)
        assertEquals(1L, recent[0].id)
    }

    @Test
    fun testToggleStickerSetInstalledNotFound() = runBlocking {
        val result = repository.toggleStickerSetInstalled(999L, true)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun testToggleStickerSetArchivedNotFound() = runBlocking {
        val result = repository.toggleStickerSetArchived(999L, true)
        assertTrue(result is Result.Failure)
    }
}
