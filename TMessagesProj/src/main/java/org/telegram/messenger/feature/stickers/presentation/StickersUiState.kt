package org.telegram.messenger.feature.stickers.presentation

import org.telegram.messenger.feature.stickers.domain.model.StickerModel
import org.telegram.messenger.feature.stickers.domain.model.StickerSetModel

/**
 * Pure Kotlin UI state for stickers and sticker sets screens/bottom sheets.
 */
sealed interface StickersUiState {
    object Loading : StickersUiState

    data class Success(
        val stickerSets: List<StickerSetModel>,
        val recentStickers: List<StickerModel> = emptyList(),
        val emojiStickers: List<StickerModel> = emptyList(),
        val selectedEmoji: String = ""
    ) : StickersUiState

    data class Error(val message: String?) : StickersUiState
}
