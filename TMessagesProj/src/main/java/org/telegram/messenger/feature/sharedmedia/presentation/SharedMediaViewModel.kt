package org.telegram.messenger.feature.sharedmedia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.messenger.feature.sharedmedia.domain.repository.SharedMediaRepository
import org.telegram.messenger.feature.sharedmedia.domain.usecase.ClearMediaSelectionUseCase
import org.telegram.messenger.feature.sharedmedia.domain.usecase.ObserveSharedMediaStateUseCase
import org.telegram.messenger.feature.sharedmedia.domain.usecase.SelectSharedMediaTabUseCase
import org.telegram.messenger.feature.sharedmedia.domain.usecase.SetSharedMediaFilterUseCase
import org.telegram.messenger.feature.sharedmedia.domain.usecase.ToggleMediaSelectionUseCase

/**
 * ViewModel для управления состоянием вкладок, фильтрации и выбора элементов общего медиа.
 */
class SharedMediaViewModel(
    private val observeSharedMediaStateUseCase: ObserveSharedMediaStateUseCase,
    private val selectSharedMediaTabUseCase: SelectSharedMediaTabUseCase,
    private val setSharedMediaFilterUseCase: SetSharedMediaFilterUseCase,
    private val toggleMediaSelectionUseCase: ToggleMediaSelectionUseCase,
    private val clearMediaSelectionUseCase: ClearMediaSelectionUseCase,
    private val repository: SharedMediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedMediaUiState())
    val uiState: StateFlow<SharedMediaUiState> = _uiState.asStateFlow()

    init {
        observeSharedMediaStateUseCase()
            .onEach { domainState ->
                _uiState.value = SharedMediaUiState(
                    dialogId = domainState.dialogId,
                    isEncrypted = domainState.isEncrypted,
                    currentTab = domainState.currentTab,
                    filterType = domainState.filterType,
                    availableTabs = domainState.availableTabs,
                    items = domainState.items,
                    sections = domainState.sections,
                    periods = domainState.periods,
                    selection = domainState.selection,
                    isLoading = domainState.isLoading,
                    hasMore = domainState.hasMore
                )
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SharedMediaEvent) {
        when (event) {
            is SharedMediaEvent.OnDialogConfigured -> {
                repository.setDialog(event.dialogId, event.isEncrypted)
            }
            is SharedMediaEvent.OnAvailableTabsUpdated -> {
                repository.setAvailableTabs(event.tabs)
            }
            is SharedMediaEvent.OnTabSelected -> {
                selectSharedMediaTabUseCase(event.tab)
            }
            is SharedMediaEvent.OnFilterChanged -> {
                setSharedMediaFilterUseCase(event.filter)
            }
            is SharedMediaEvent.OnItemsLoaded -> {
                repository.setItems(event.items, event.hasMore)
            }
            is SharedMediaEvent.OnMoreItemsLoaded -> {
                repository.addItems(event.items, event.hasMore)
            }
            is SharedMediaEvent.OnItemSelectionToggled -> {
                toggleMediaSelectionUseCase(event.messageId)
            }
            is SharedMediaEvent.OnSelectAllRequested -> {
                repository.selectAll()
            }
            is SharedMediaEvent.OnClearSelectionRequested -> {
                clearMediaSelectionUseCase()
            }
            is SharedMediaEvent.OnDeleteSelectedRequested -> {
                repository.deleteSelectedItems()
            }
            is SharedMediaEvent.OnClearRequested -> {
                repository.clear()
            }
        }
    }
}
