package org.telegram.messenger.feature.media.sharedmedia.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.sharedmedia.data.datasource.SharedMediaLocalDataSource
import org.telegram.messenger.feature.media.sharedmedia.data.datasource.SharedMediaRemoteDataSource
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaFilterType
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaItem
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaState
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaTabSpec
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaTabType
import org.telegram.messenger.feature.media.sharedmedia.domain.repository.SharedMediaRepository

class SharedMediaRepositoryImpl(
    private val currentAccount: Int = 0,
    private val localDataSource: SharedMediaLocalDataSource,
    private val remoteDataSource: SharedMediaRemoteDataSource
) : SharedMediaRepository {

    override fun observeState(): Flow<SharedMediaState> = localDataSource.state

    override fun getState(): SharedMediaState = localDataSource.getState()

    override fun setDialog(dialogId: Long, isEncrypted: Boolean) {
        localDataSource.setDialog(dialogId, isEncrypted)
    }

    override fun setAvailableTabs(tabs: List<SharedMediaTabSpec>) {
        localDataSource.setAvailableTabs(tabs)
    }

    override fun selectTab(tab: SharedMediaTabType) {
        localDataSource.selectTab(tab)
    }

    override fun setFilter(filter: SharedMediaFilterType) {
        localDataSource.setFilter(filter)
    }

    override fun setItems(items: List<SharedMediaItem>, hasMore: Boolean) {
        localDataSource.setItems(items, hasMore)
    }

    override fun addItems(items: List<SharedMediaItem>, hasMore: Boolean) {
        localDataSource.addItems(items, hasMore)
    }

    override fun toggleItemSelection(messageId: Int) {
        localDataSource.toggleItemSelection(messageId)
    }

    override fun selectAll() {
        localDataSource.selectAll()
    }

    override fun clearSelection() {
        localDataSource.clearSelection()
    }

    override fun deleteSelectedItems(): List<Int> {
        return localDataSource.deleteSelectedItems()
    }

    override fun setLoading(loading: Boolean) {
        localDataSource.setLoading(loading)
    }

    override fun clear() {
        localDataSource.clear()
    }
}
