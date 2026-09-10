package org.telegram.messenger.feature.emojipicker.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.emojipicker.domain.repository.EmojiPickerRepository
import org.telegram.messenger.feature.emojipicker.domain.usecase.ClearRecentPickerItemsUseCase
import org.telegram.messenger.feature.emojipicker.domain.usecase.GetEmojiPickerStateUseCase
import org.telegram.messenger.feature.emojipicker.domain.usecase.ObserveEmojiPickerStateUseCase
import org.telegram.messenger.feature.emojipicker.domain.usecase.SelectPickerTabUseCase
import org.telegram.messenger.feature.emojipicker.domain.usecase.ToggleStickerFavoriteUseCase
import org.telegram.messenger.feature.emojipicker.domain.usecase.UpdatePickerSearchQueryUseCase

class EmojiPickerViewModel(
    private val observeStateUseCase: ObserveEmojiPickerStateUseCase,
    private val getStateUseCase: GetEmojiPickerStateUseCase,
    private val selectTabUseCase: SelectPickerTabUseCase,
    private val updateSearchQueryUseCase: UpdatePickerSearchQueryUseCase,
    private val toggleFavoriteUseCase: ToggleStickerFavoriteUseCase,
    private val clearRecentUseCase: ClearRecentPickerItemsUseCase,
    private val repository: EmojiPickerRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _uiState = MutableStateFlow(EmojiPickerUiState())
    val uiState: StateFlow<EmojiPickerUiState> = _uiState.asStateFlow()

    init {
        observeStateUseCase().onEach { domainState ->
            _uiState.update { current ->
                current.copy(
                    currentTab = domainState.currentTab,
                    availableTabs = domainState.availableTabs,
                    filter = domainState.filter,
                    searchQuery = domainState.searchQuery,
                    isSearchActive = domainState.isSearchActive,
                    isLoading = domainState.isLoading,
                    recentEmojis = domainState.recentEmojis,
                    installedStickerPacks = domainState.installedStickerPacks,
                    recentStickers = domainState.recentStickers,
                    favoriteStickers = domainState.favoriteStickers,
                    recentGifs = domainState.recentGifs,
                    trendingGifs = domainState.trendingGifs
                )
            }
        }.launchIn(scope)
    }

    fun onEvent(event: EmojiPickerEvent) {
        scope.launch {
            when (event) {
                is EmojiPickerEvent.OnFilterConfigured -> {
                    repository.configureFilter(event.filter)
                }
                is EmojiPickerEvent.OnTabSelected -> {
                    selectTabUseCase(event.tab)
                }
                is EmojiPickerEvent.OnSearchQueryChanged -> {
                    updateSearchQueryUseCase(event.query)
                }
                is EmojiPickerEvent.OnSearchActiveChanged -> {
                    repository.setSearchActive(event.isActive)
                }
                is EmojiPickerEvent.OnEmojiSelected -> {
                    _uiState.update { it.copy(selectedEmoji = event.emoji) }
                }
                is EmojiPickerEvent.OnStickerSelected -> {
                    _uiState.update { it.copy(selectedSticker = event.sticker) }
                }
                is EmojiPickerEvent.OnGifSelected -> {
                    _uiState.update { it.copy(selectedGif = event.gif) }
                }
                is EmojiPickerEvent.OnStickerFavoriteToggled -> {
                    toggleFavoriteUseCase(event.stickerId)
                }
                is EmojiPickerEvent.OnClearRecentRequested -> {
                    clearRecentUseCase(event.tab)
                }
                is EmojiPickerEvent.OnClearRequested -> {
                    repository.clear()
                    _uiState.update {
                        EmojiPickerUiState(
                            selectedEmoji = null,
                            selectedSticker = null,
                            selectedGif = null
                        )
                    }
                }
            }
        }
    }
}
