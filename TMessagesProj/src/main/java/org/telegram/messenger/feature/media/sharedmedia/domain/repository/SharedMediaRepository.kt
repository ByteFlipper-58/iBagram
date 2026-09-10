package org.telegram.messenger.feature.media.sharedmedia.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaFilterType
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaItem
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaState
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaTabSpec
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaTabType

/**
 * Чистый интерфейс репозитория для управления общим медиа диалогов.
 */
interface SharedMediaRepository {
    fun observeState(): Flow<SharedMediaState>
    fun getState(): SharedMediaState
    fun setDialog(dialogId: Long, isEncrypted: Boolean)
    fun setAvailableTabs(tabs: List<SharedMediaTabSpec>)
    fun selectTab(tab: SharedMediaTabType)
    fun setFilter(filter: SharedMediaFilterType)
    fun setItems(items: List<SharedMediaItem>, hasMore: Boolean)
    fun addItems(items: List<SharedMediaItem>, hasMore: Boolean)
    fun toggleItemSelection(messageId: Int)
    fun selectAll()
    fun clearSelection()
    fun deleteSelectedItems(): List<Int>
    fun setLoading(isLoading: Boolean)
    fun clear()
}
