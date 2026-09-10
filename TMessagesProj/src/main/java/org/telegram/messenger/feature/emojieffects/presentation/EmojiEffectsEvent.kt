package org.telegram.messenger.feature.emojieffects.presentation

import org.telegram.messenger.feature.emojieffects.domain.model.EmojiEffectItem

/**
 * UI-события для управления воспроизведением эффектов и сессиями тапов.
 */
sealed interface EmojiEffectsEvent {
    data class OnEmojiTapped(
        val messageId: Int,
        val rawEmoji: String,
        val animationIndex: Int,
        val timestampMs: Long
    ) : EmojiEffectsEvent

    data class OnEffectStarted(val item: EmojiEffectItem) : EmojiEffectsEvent
    data class OnEffectProgressUpdated(val id: String, val progress: Float) : EmojiEffectsEvent
    data class OnEffectDismissed(val id: String) : EmojiEffectsEvent
    object OnCancelAllRequested : EmojiEffectsEvent
    object OnFlushTapsRequested : EmojiEffectsEvent
    object OnClearRequested : EmojiEffectsEvent
}
