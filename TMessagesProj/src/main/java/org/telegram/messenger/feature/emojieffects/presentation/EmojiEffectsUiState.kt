package org.telegram.messenger.feature.emojieffects.presentation

import org.telegram.messenger.feature.emojieffects.domain.model.EmojiEffectItem

/**
 * UI-состояние оверлея интерактивных анимаций эмодзи.
 */
data class EmojiEffectsUiState(
    val activeEffects: List<EmojiEffectItem> = emptyList(),
    val isIdle: Boolean = true,
    val pendingInteractionsCount: Int = 0,
    val lastRecordedEmoji: String? = null
)
