package org.telegram.messenger.feature.messaging.emojipicker.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.messaging.emojipicker.data.datasource.EmojiPickerLocalDataSource
import org.telegram.messenger.feature.messaging.emojipicker.data.datasource.EmojiPickerRemoteDataSource
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiItem
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerFilter
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerState
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerTabType
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.GifItem
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.StickerItem
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.StickerPackItem
import org.telegram.messenger.feature.messaging.emojipicker.domain.repository.EmojiPickerRepository

class EmojiPickerRepositoryImpl(
    private val currentAccount: Int = 0,
    private val localDataSource: EmojiPickerLocalDataSource,
    private val remoteDataSource: EmojiPickerRemoteDataSource
) : EmojiPickerRepository {

    override fun observeState(): StateFlow<EmojiPickerState> = localDataSource.state

    override fun getState(): EmojiPickerState = localDataSource.getState()

    override fun configureFilter(filter: EmojiPickerFilter) {
        localDataSource.configureFilter(filter)
    }

    override fun selectTab(tab: EmojiPickerTabType) {
        localDataSource.selectTab(tab)
    }

    override fun setSearchQuery(query: String) {
        localDataSource.setSearchQuery(query)
    }

    override fun setSearchActive(isActive: Boolean) {
        localDataSource.setSearchActive(isActive)
    }

    override fun setRecentEmojis(emojis: List<EmojiItem>) {
        localDataSource.setRecentEmojis(emojis)
    }

    override fun setStickerPacks(packs: List<StickerPackItem>) {
        localDataSource.setStickerPacks(packs)
    }

    override fun setRecentStickers(stickers: List<StickerItem>) {
        localDataSource.setRecentStickers(stickers)
    }

    override fun setFavoriteStickers(stickers: List<StickerItem>) {
        localDataSource.setFavoriteStickers(stickers)
    }

    override fun toggleStickerFavorite(stickerId: Long) {
        localDataSource.toggleStickerFavorite(stickerId)
    }

    override fun setRecentGifs(gifs: List<GifItem>) {
        localDataSource.setRecentGifs(gifs)
    }

    override fun setTrendingGifs(gifs: List<GifItem>) {
        localDataSource.setTrendingGifs(gifs)
    }

    override fun clearRecent(tab: EmojiPickerTabType) {
        localDataSource.clearRecent(tab)
    }

    override fun setLoading(isLoading: Boolean) {
        localDataSource.setLoading(isLoading)
    }

    override fun clear() {
        localDataSource.clear()
    }
}
