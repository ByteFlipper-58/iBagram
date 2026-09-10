package org.telegram.messenger.feature.messaging.emojieffects.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiEffectItem
import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiEffectsState
import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiInteractionSession

/**
 * Контракт репозитория для управления интерактивными эффектами эмодзи и сессиями тапов.
 */
interface EmojiEffectsRepository {
    fun observeState(): Flow<EmojiEffectsState>
    fun getState(): EmojiEffectsState
    fun recordTap(messageId: Int, emoticon: String, animationIndex: Int, timestampMs: Long): EmojiInteractionSession
    fun drainCurrentSession(): EmojiInteractionSession?
    fun clearCurrentSession()
    fun startEffect(item: EmojiEffectItem)
    fun updateEffectProgress(id: String, progress: Float)
    fun dismissEffect(id: String)
    fun removeEffect(id: String)
    fun cancelAllEffects()
    fun clear()
    fun updateLastAnimationIndex(documentId: Long, nextIndex: Int)
    fun getLastAnimationIndex(documentId: Long): Int
}
