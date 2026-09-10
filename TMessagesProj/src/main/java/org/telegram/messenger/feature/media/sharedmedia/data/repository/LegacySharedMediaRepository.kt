package org.telegram.messenger.feature.media.sharedmedia.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.media.sharedmedia.data.mapper.SharedMediaMapper
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaFilterType
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaItem
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaState
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaTabSpec
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaTabType
import org.telegram.messenger.feature.media.sharedmedia.domain.repository.SharedMediaRepository
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.CalculateMediaSelectionUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.FilterSharedMediaUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.GroupMediaByMonthUseCase

/**
 * Потокобезопасная реализация SharedMediaRepository.
 */
class LegacySharedMediaRepository(
    private val groupMediaByMonthUseCase: GroupMediaByMonthUseCase = GroupMediaByMonthUseCase(),
    private val calculateMediaSelectionUseCase: CalculateMediaSelectionUseCase = CalculateMediaSelectionUseCase(),
    private val filterSharedMediaUseCase: FilterSharedMediaUseCase = FilterSharedMediaUseCase()
) : SharedMediaRepository {

    private val lock = Any()
    private val rawItems = mutableListOf<SharedMediaItem>()
    private val _state = MutableStateFlow(SharedMediaState())

    override fun observeState(): Flow<SharedMediaState> = _state.asStateFlow()

    override fun getState(): SharedMediaState = synchronized(lock) { _state.value }

    override fun setDialog(dialogId: Long, isEncrypted: Boolean) {
        synchronized(lock) {
            rawItems.clear()
            _state.value = SharedMediaState(
                dialogId = dialogId,
                isEncrypted = isEncrypted
            )
        }
    }

    override fun setAvailableTabs(tabs: List<SharedMediaTabSpec>) {
        synchronized(lock) {
            _state.value = _state.value.copy(availableTabs = tabs)
        }
    }

    override fun selectTab(tab: SharedMediaTabType) {
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

    override fun setFilter(filter: SharedMediaFilterType) {
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

    override fun setItems(items: List<SharedMediaItem>, hasMore: Boolean) {
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

    override fun addItems(items: List<SharedMediaItem>, hasMore: Boolean) {
        synchronized(lock) {
            val existingIds = rawItems.map { it.messageId }.toSet()
            val newUnique = items.filter { it.messageId !in existingIds }
            rawItems.addAll(newUnique)

            val filtered = filterSharedMediaUseCase(rawItems, _state.value.filterType)
            val sections = groupMediaByMonthUseCase(filtered)
            val periods = SharedMediaMapper.calculateFastScrollPeriods(filtered)

            _state.value = _state.value.copy(
                items = filtered,
                sections = sections,
                periods = periods,
                isLoading = false,
                hasMore = hasMore
            )
        }
    }

    override fun toggleItemSelection(messageId: Int) {
        synchronized(lock) {
            val currentSelected = _state.value.selection.selectedIds.toMutableSet()
            if (currentSelected.contains(messageId)) {
                currentSelected.remove(messageId)
            } else {
                currentSelected.add(messageId)
            }
            val selection = calculateMediaSelectionUseCase(_state.value.items, currentSelected)
            _state.value = _state.value.copy(selection = selection)
        }
    }

    override fun selectAll() {
        synchronized(lock) {
            val allIds = _state.value.items.map { it.messageId }.toSet()
            val selection = calculateMediaSelectionUseCase(_state.value.items, allIds)
            _state.value = _state.value.copy(selection = selection)
        }
    }

    override fun clearSelection() {
        synchronized(lock) {
            _state.value = _state.value.copy(
                selection = calculateMediaSelectionUseCase(_state.value.items, emptySet())
            )
        }
    }

    override fun deleteSelectedItems(): List<Int> {
        synchronized(lock) {
            val toDelete = _state.value.selection.selectedIds.toList()
            if (toDelete.isEmpty()) return emptyList()

            rawItems.removeAll { it.messageId in toDelete }
            val filtered = filterSharedMediaUseCase(rawItems, _state.value.filterType)
            val sections = groupMediaByMonthUseCase(filtered)
            val periods = SharedMediaMapper.calculateFastScrollPeriods(filtered)
            val selection = calculateMediaSelectionUseCase(filtered, emptySet())

            _state.value = _state.value.copy(
                items = filtered,
                sections = sections,
                periods = periods,
                selection = selection
            )
            return toDelete
        }
    }

    override fun setLoading(isLoading: Boolean) {
        synchronized(lock) {
            _state.value = _state.value.copy(isLoading = isLoading)
        }
    }

    override fun clear() {
        synchronized(lock) {
            rawItems.clear()
            _state.value = SharedMediaState()
        }
    }
}
