package org.telegram.messenger.feature.messaging.emojipicker

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.emojipicker.data.datasource.EmojiPickerLocalDataSource
import org.telegram.messenger.feature.messaging.emojipicker.data.datasource.EmojiPickerRemoteDataSource
import org.telegram.messenger.feature.messaging.emojipicker.data.repository.EmojiPickerRepositoryImpl
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiItem
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerTabType
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.StickerItem

class EmojiPickerRepositoryImplTest {

    private lateinit var localDataSource: EmojiPickerLocalDataSource
    private lateinit var remoteDataSource: EmojiPickerRemoteDataSource
    private lateinit var repository: EmojiPickerRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = EmojiPickerLocalDataSource(0)
        remoteDataSource = EmojiPickerRemoteDataSource(0)
        repository = EmojiPickerRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource
        )
    }

    @Test
    fun initialState_hasDefaults() {
        val state = repository.getState()
        assertEquals(EmojiPickerTabType.EMOJI, state.currentTab)
        assertEquals("", state.searchQuery)
        assertFalse(state.isSearchActive)
        assertTrue(state.recentEmojis.isEmpty())
    }

    @Test
    fun selectTab_updatesCurrentTab() = runBlocking {
        repository.selectTab(EmojiPickerTabType.STICKERS)
        val state = repository.observeState().first()
        assertEquals(EmojiPickerTabType.STICKERS, state.currentTab)
    }

    @Test
    fun setSearchQuery_updatesQuery() {
        repository.setSearchQuery("smile")
        assertEquals("smile", repository.getState().searchQuery)

        repository.setSearchActive(true)
        assertTrue(repository.getState().isSearchActive)
    }

    @Test
    fun setRecentEmojis_updatesList() {
        val emojis = listOf(EmojiItem(id = "1", unicode = "😀"), EmojiItem(id = "2", unicode = "🎉"))
        repository.setRecentEmojis(emojis)

        val state = repository.getState()
        assertEquals(2, state.recentEmojis.size)
        assertEquals("😀", state.recentEmojis[0].unicode)
    }

    @Test
    fun toggleStickerFavorite_addsAndRemoves() {
        val sticker = StickerItem(id = 100L)
        repository.setRecentStickers(listOf(sticker))

        repository.toggleStickerFavorite(100L)
        var state = repository.getState()
        assertEquals(1, state.favoriteStickers.size)
        assertEquals(100L, state.favoriteStickers[0].id)

        repository.toggleStickerFavorite(100L)
        state = repository.getState()
        assertTrue(state.favoriteStickers.isEmpty())
    }

    @Test
    fun clearRecent_clearsTargetCategory() {
        repository.setRecentEmojis(listOf(EmojiItem(id = "1")))
        repository.setRecentStickers(listOf(StickerItem(id = 2L)))

        repository.clearRecent(EmojiPickerTabType.EMOJI)
        var state = repository.getState()
        assertTrue(state.recentEmojis.isEmpty())
        assertEquals(1, state.recentStickers.size)

        repository.clearRecent(EmojiPickerTabType.STICKERS)
        state = repository.getState()
        assertTrue(state.recentStickers.isEmpty())
    }

    @Test
    fun clear_resetsAll() {
        repository.setSearchQuery("query")
        repository.setRecentEmojis(listOf(EmojiItem(id = "10")))
        repository.clear()

        val state = repository.getState()
        assertEquals("", state.searchQuery)
        assertTrue(state.recentEmojis.isEmpty())
    }
}
