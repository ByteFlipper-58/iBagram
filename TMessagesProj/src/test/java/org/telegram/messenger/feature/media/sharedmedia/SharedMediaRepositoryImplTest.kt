package org.telegram.messenger.feature.media.sharedmedia

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.media.sharedmedia.data.datasource.SharedMediaLocalDataSource
import org.telegram.messenger.feature.media.sharedmedia.data.datasource.SharedMediaRemoteDataSource
import org.telegram.messenger.feature.media.sharedmedia.data.repository.SharedMediaRepositoryImpl
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaFilterType
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaItem
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaTabSpec
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaTabType

class SharedMediaRepositoryImplTest {

    private lateinit var localDataSource: SharedMediaLocalDataSource
    private lateinit var remoteDataSource: SharedMediaRemoteDataSource
    private lateinit var repository: SharedMediaRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = SharedMediaLocalDataSource(0)
        remoteDataSource = SharedMediaRemoteDataSource(0)
        repository = SharedMediaRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource
        )
    }

    @Test
    fun setDialog_initializesState() = runBlocking {
        repository.setDialog(12345L, true)
        val state = repository.observeState().first()
        assertEquals(12345L, state.dialogId)
        assertTrue(state.isEncrypted)
    }

    @Test
    fun setAvailableTabs_and_selectTab_updatesState() {
        val tabs = listOf(
            SharedMediaTabSpec(SharedMediaTabType.PHOTO_VIDEO, "Media"),
            SharedMediaTabSpec(SharedMediaTabType.FILES, "Files")
        )
        repository.setAvailableTabs(tabs)
        assertEquals(2, repository.getState().availableTabs.size)

        repository.selectTab(SharedMediaTabType.FILES)
        assertEquals(SharedMediaTabType.FILES, repository.getState().currentTab)
        assertTrue(repository.getState().isLoading)
    }

    @Test
    fun setItems_and_filtering_worksCorrectly() {
        val photo = SharedMediaItem(id = 1, messageId = 101, dialogId = 1, date = 1000L, monthKey = "2024-01", isPhoto = true)
        val video = SharedMediaItem(id = 2, messageId = 102, dialogId = 1, date = 1000L, monthKey = "2024-01", isVideo = true)
        repository.setItems(listOf(photo, video), hasMore = false)

        assertEquals(2, repository.getState().items.size)

        repository.setFilter(SharedMediaFilterType.PHOTOS_ONLY)
        assertEquals(1, repository.getState().items.size)
        assertEquals(101, repository.getState().items.first().messageId)

        repository.setFilter(SharedMediaFilterType.VIDEOS_ONLY)
        assertEquals(1, repository.getState().items.size)
        assertEquals(102, repository.getState().items.first().messageId)
    }

    @Test
    fun toggleSelection_and_clearSelection_updatesState() {
        val item1 = SharedMediaItem(id = 1, messageId = 10, dialogId = 1, date = 1000L, monthKey = "2024-01", isPhoto = true)
        val item2 = SharedMediaItem(id = 2, messageId = 20, dialogId = 1, date = 1000L, monthKey = "2024-01", isPhoto = true)
        repository.setItems(listOf(item1, item2), hasMore = false)

        repository.toggleItemSelection(10)
        assertTrue(repository.getState().selection.isSelectionActive)
        assertEquals(1, repository.getState().selection.count)

        repository.toggleItemSelection(20)
        assertEquals(2, repository.getState().selection.count)

        repository.clearSelection()
        assertFalse(repository.getState().selection.isSelectionActive)
    }
}
