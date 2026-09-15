package org.telegram.messenger.feature.messaging.emojieffects.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiEffectItem
import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiEffectsState
import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiInteractionSession

class EmojiEffectsLocalDataSource(
    private val currentAccount: Int = 0
) {
    private val lock = Any()
    private val _state = MutableStateFlow(EmojiEffectsState())
    val stateFlow: Flow<EmojiEffectsState> = _state.asStateFlow()

    fun getState(): EmojiEffectsState = synchronized(lock) { _state.value }

    fun recordTap(
        messageId: Int,
        emoticon: String,
        animationIndex: Int,
        timestampMs: Long
    ): EmojiInteractionSession = synchronized(lock) {
        val current = _state.value
        val existingSession = current.currentSession

        val updatedSession = if (existingSession == null || existingSession.messageId != messageId) {
            EmojiInteractionSession(
                messageId = messageId,
                emoticon = emoticon,
                initialTimestampMs = timestampMs,
                relativeTimeOffsetsMs = listOf(0L),
                animationIndexes = listOf(animationIndex)
            )
        } else {
            val offset = (timestampMs - existingSession.initialTimestampMs).coerceAtLeast(0L)
            existingSession.copy(
                relativeTimeOffsetsMs = existingSession.relativeTimeOffsetsMs + offset,
                animationIndexes = existingSession.animationIndexes + animationIndex
            )
        }

        _state.value = current.copy(
            currentSession = updatedSession
        )
        updatedSession
    }

    fun drainCurrentSession(): EmojiInteractionSession? = synchronized(lock) {
        val current = _state.value
        val session = current.currentSession
        if (session != null) {
            _state.value = current.copy(currentSession = null)
        }
        session
    }

    fun clearCurrentSession() = synchronized(lock) {
        val current = _state.value
        if (current.currentSession != null) {
            _state.value = current.copy(currentSession = null)
        }
    }

    fun startEffect(item: EmojiEffectItem) = synchronized(lock) {
        val current = _state.value
        val updated = current.activeEffects.filterNot { it.id == item.id } + item
        _state.value = current.copy(
            activeEffects = updated,
            isIdle = updated.isEmpty()
        )
    }

    fun updateEffectProgress(id: String, progress: Float) = synchronized(lock) {
        val current = _state.value
        val updated = current.activeEffects.map { effect ->
            if (effect.id == id) {
                effect.copy(progress = progress)
            } else {
                effect
            }
        }
        _state.value = current.copy(
            activeEffects = updated,
            isIdle = updated.isEmpty()
        )
    }

    fun dismissEffect(id: String) = synchronized(lock) {
        val current = _state.value
        val updated = current.activeEffects.map { effect ->
            if (effect.id == id) {
                effect.copy(isRemoving = true)
            } else {
                effect
            }
        }
        _state.value = current.copy(activeEffects = updated)
    }

    fun removeEffect(id: String) = synchronized(lock) {
        val current = _state.value
        val updated = current.activeEffects.filterNot { it.id == id }
        _state.value = current.copy(
            activeEffects = updated,
            isIdle = updated.isEmpty()
        )
    }

    fun cancelAllEffects() = synchronized(lock) {
        val current = _state.value
        _state.value = current.copy(
            activeEffects = emptyList(),
            isIdle = true
        )
    }

    fun clear() = synchronized(lock) {
        _state.value = EmojiEffectsState()
    }

    fun updateLastAnimationIndex(documentId: Long, nextIndex: Int) = synchronized(lock) {
        val current = _state.value
        val updatedMap = current.lastAnimationIndexPerDocument + (documentId to nextIndex)
        _state.value = current.copy(lastAnimationIndexPerDocument = updatedMap)
    }

    fun getLastAnimationIndex(documentId: Long): Int = synchronized(lock) {
        return _state.value.lastAnimationIndexPerDocument[documentId] ?: 0
    }
}
