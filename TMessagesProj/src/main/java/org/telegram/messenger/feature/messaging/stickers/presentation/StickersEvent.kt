package org.telegram.messenger.feature.messaging.stickers.presentation

/**
 * Single-shot UI events emitted by [StickersViewModel].
 */
sealed interface StickersEvent {
    data class StickerSetInstalled(val setId: Long) : StickersEvent
    data class StickerSetUninstalled(val setId: Long) : StickersEvent
    data class StickerSetArchived(val setId: Long) : StickersEvent
    data class ShowError(val message: String) : StickersEvent
}
