package org.telegram.messenger.feature.stickers.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.stickers.domain.usecase.GetRecentStickersUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.GetStickerSetUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.GetStickerSetsUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.GetStickersForEmojiUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.ObserveStickerSetsUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.ToggleStickerSetArchivedUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.ToggleStickerSetInstalledUseCase

/**
 * ViewModel managing stickers, sticker sets, recent stickers, and emoji suggestions.
 */
class StickersViewModel(
    private val observeStickerSetsUseCase: ObserveStickerSetsUseCase,
    private val getStickerSetsUseCase: GetStickerSetsUseCase,
    private val getStickerSetUseCase: GetStickerSetUseCase,
    private val getRecentStickersUseCase: GetRecentStickersUseCase,
    private val getStickersForEmojiUseCase: GetStickersForEmojiUseCase,
    private val toggleStickerSetInstalledUseCase: ToggleStickerSetInstalledUseCase,
    private val toggleStickerSetArchivedUseCase: ToggleStickerSetArchivedUseCase,
    private val stickerType: Int = 0
) : ViewModel() {

    private val _uiState = MutableStateFlow<StickersUiState>(StickersUiState.Loading)
    val uiState: StateFlow<StickersUiState> = _uiState.asStateFlow()

    private val _events = Channel<StickersEvent>(Channel.BUFFERED)
    val events: Flow<StickersEvent> = _events.receiveAsFlow()

    init {
        observeStickerSets()
        loadRecentStickers()
    }

    private fun observeStickerSets() {
        viewModelScope.launch {
            observeStickerSetsUseCase(stickerType).collect { sets ->
                val current = _uiState.value
                val recent = if (current is StickersUiState.Success) current.recentStickers else emptyList()
                val emojiStickers = if (current is StickersUiState.Success) current.emojiStickers else emptyList()
                val selectedEmoji = if (current is StickersUiState.Success) current.selectedEmoji else ""
                _uiState.value = StickersUiState.Success(
                    stickerSets = sets,
                    recentStickers = recent,
                    emojiStickers = emojiStickers,
                    selectedEmoji = selectedEmoji
                )
            }
        }
    }

    fun loadRecentStickers() {
        viewModelScope.launch {
            try {
                val recent = getRecentStickersUseCase(stickerType)
                val current = _uiState.value
                if (current is StickersUiState.Success) {
                    _uiState.value = current.copy(recentStickers = recent)
                }
            } catch (_: Throwable) {
                // Ignore failure for recent stickers
            }
        }
    }

    fun searchEmoji(emoji: String) {
        val current = _uiState.value
        if (current is StickersUiState.Success) {
            viewModelScope.launch {
                if (emoji.isBlank()) {
                    _uiState.value = current.copy(emojiStickers = emptyList(), selectedEmoji = "")
                } else {
                    val matching = getStickersForEmojiUseCase(emoji)
                    _uiState.value = current.copy(emojiStickers = matching, selectedEmoji = emoji)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val sets = getStickerSetsUseCase(stickerType)
                val recent = getRecentStickersUseCase(stickerType)
                val current = _uiState.value
                val emojiStickers = if (current is StickersUiState.Success) current.emojiStickers else emptyList()
                val selectedEmoji = if (current is StickersUiState.Success) current.selectedEmoji else ""
                _uiState.value = StickersUiState.Success(
                    stickerSets = sets,
                    recentStickers = recent,
                    emojiStickers = emojiStickers,
                    selectedEmoji = selectedEmoji
                )
            } catch (e: Throwable) {
                _uiState.value = StickersUiState.Error(e.message)
            }
        }
    }

    fun installStickerSet(id: Long) {
        viewModelScope.launch {
            when (val result = toggleStickerSetInstalledUseCase(id, true)) {
                is Result.Success -> _events.send(StickersEvent.StickerSetInstalled(id))
                is Result.Failure -> _events.send(StickersEvent.ShowError(result.error.message))
            }
        }
    }

    fun uninstallStickerSet(id: Long) {
        viewModelScope.launch {
            when (val result = toggleStickerSetInstalledUseCase(id, false)) {
                is Result.Success -> _events.send(StickersEvent.StickerSetUninstalled(id))
                is Result.Failure -> _events.send(StickersEvent.ShowError(result.error.message))
            }
        }
    }

    fun archiveStickerSet(id: Long) {
        viewModelScope.launch {
            when (val result = toggleStickerSetArchivedUseCase(id, true)) {
                is Result.Success -> _events.send(StickersEvent.StickerSetArchived(id))
                is Result.Failure -> _events.send(StickersEvent.ShowError(result.error.message))
            }
        }
    }
}
