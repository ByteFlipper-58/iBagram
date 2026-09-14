package org.telegram.messenger.feature.media.sharedmedia.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.media.sharedmedia.data.mapper.SharedMediaMapper
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaFilterType
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaItem
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaState
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaTabSpec
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaTabType
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.CalculateMediaSelectionUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.FilterSharedMediaUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.GroupMediaByMonthUseCase

class SharedMediaLocalDataSource(
    private val currentAccount: Int = 0,
    private val groupMediaByMonthUseCase: GroupMediaByMonthUseCase = GroupMediaByMonthUseCase(),
    private val calculateMediaSelectionUseCase: CalculateMediaSelectionUseCase = CalculateMediaSelectionUseCase(),
    private val filterSharedMediaUseCase: FilterSharedMediaUseCase = FilterSharedMediaUseCase()
) {
    private val lock = Any()
    private val rawItems = mutableListOf<SharedMediaItem>()
    private val _state = MutableStateFlow(SharedMediaState())
    val state: StateFlow<SharedMediaState> = _state.asStateFlow()

    fun getState(): SharedMediaState = synchronized(lock) { _state.value }

    fun setDialog(dialogId: Long, isEncrypted: Boolean) {
        synchronized(lock) {
            rawItems.clear()
            _state.value = SharedMediaState(
                dialogId = dialogId,
                isEncrypted = isEncrypted
            )
        }
    }

    fun setAvailableTabs(tabs: List<SharedMediaTabSpec>) {
        synchronized(lock) {
            _state.value = _state.value.copy(availableTabs = tabs)
        }
    }

    fun selectTab(tab: SharedMediaTabType) {
        synchronized(lock) {
            if (_state.value.currentTab == tab) return
            rawItems.clear()
            _state.value = _state.value.copy(
                currentTab = tab,
                items = emptyList(),
                sections = emptyMap(),
                periods = emptyList(),
                selection = calculateMediaSelectionUseCase(emptyList(), emptySet()),
                isLoading = true,
                hasMore = true
            )
        }
    }

    fun setFilter(filter: SharedMediaFilterType) {
        synchronized(lock) {
            if (_state.value.filterType == filter) return
            val filtered = filterSharedMediaUseCase(rawItems, filter)
            val sections = groupMediaByMonthUseCase(filtered)
            val periods = SharedMediaMapper.calculateFastScrollPeriods(filtered)
            val currentSelected = _state.value.selection.selectedIds.filter { id ->
                filtered.any { it.messageId == id }
            }.toSet()
            val selection = calculateMediaSelectionUseCase(filtered, currentSelected)

            _state.value = _state.value.copy(
                filterType = filter,
                items = filtered,
                sections = sections,
                periods = periods,
                selection = selection
            )
        }
    }

    fun setItems(items: List<SharedMediaItem>, hasMore: Boolean) {
        synchronized(lock) {
            rawItems.clear()
            rawItems.addAll(items)
            val filtered = filterSharedMediaUseCase(rawItems, _state.value.filterType)
            val sections = groupMediaByMonthUseCase(filtered)
            val periods = SharedMediaMapper.calculateFastScrollPeriods(filtered)
            val selection = calculateMediaSelectionUseCase(filtered, emptySet())

            _state.value = _state.value.copy(
                items = filtered,
                sections = sections,
                periods = periods,
                selection = selection,
                isLoading = false,
                hasMore = hasMore
            )
        }
    }

    fun appendItems(items: List<SharedMediaItem>, hasMore: Boolean) {
        synchronized(lock) {
            val existingIds = rawItems.map { it.id }.toSet()
            val newItems = items.filter { it.id !in existingIds }
            rawItems.addAll(newItems)

            val filtered = filterSharedMediaUseCase(rawItems, _state.value.filterType)
            val sections = groupMediaByMonthUseCase(filtered)
            val periods = SharedMediaMapper.calculateFastScrollPeriods(filtered)
            val currentSelected = _state.value.selection.selectedIds
            val selection = calculateMediaSelectionUseCase(filtered, currentSelected)

            _state.value = _state.value.copy(
                items = filtered,
                sections = sections,
                periods = periods,
                selection = selection,
                isLoading = false,
                hasMore = hasMore
            )
        }
    }

    fun toggleItemSelection(messageId: Int) {
        synchronized(lock) {
            val current = _state.value
            val selected = current.selection.selectedIds.toMutableSet()
            if (selected.contains(messageId)) {
                selected.remove(messageId)
            } else {
                selected.add(messageId)
            }
            val newSelection = calculateMediaSelectionUseCase(current.items, selected)
            _state.value = current.copy(selection = newSelection)
        }
    }

    fun addItems(items: List<SharedMediaItem>, hasMore: Boolean) {
        appendItems(items, hasMore)
    }

    fun selectAll() {
        synchronized(lock) {
            val current = _state.value
            val allIds = current.items.map { it.messageId }.toSet()
            val newSelection = calculateMediaSelectionUseCase(current.items, allIds)
            _state.value = current.copy(selection = newSelection)
        }
    }

    fun clearSelection() {
        synchronized(lock) {
            val current = _state.value
            if (current.selection.selectedIds.isEmpty()) return
            val newSelection = calculateMediaSelectionUseCase(current.items, emptySet())
            _state.value = current.copy(selection = newSelection)
        }
    }

    fun deleteSelectedItems(): List<Int> {
        return synchronized(lock) {
            val current = _state.value
            val selectedIds = current.selection.selectedIds.toList()
            rawItems.removeAll { it.messageId in selectedIds }
            val filtered = filterSharedMediaUseCase(rawItems, current.filterType)
            val sections = groupMediaByMonthUseCase(filtered)
            val periods = SharedMediaMapper.calculateFastScrollPeriods(filtered)
            val newSelection = calculateMediaSelectionUseCase(filtered, emptySet())
            _state.value = current.copy(
                items = filtered,
                sections = sections,
                periods = periods,
                selection = newSelection
            )
            selectedIds
        }
    }

    fun setLoading(loading: Boolean) {
        synchronized(lock) {
            _state.value = _state.value.copy(isLoading = loading)
        }
    }

    fun clear() {
        synchronized(lock) {
            rawItems.clear()
            _state.value = SharedMediaState()
        }
    }
}
