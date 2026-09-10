package org.telegram.messenger.feature.messaging.emojieffects.domain.usecase

import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiInteractionSession
import org.telegram.messenger.feature.messaging.emojieffects.domain.repository.EmojiEffectsRepository

/**
 * Фиксирует тап пользователя по эмодзи в сообщении, агрегируя временные интервалы
 * и индексы анимаций для последующей отправки собеседнику.
 */
class RecordEmojiTapUseCase(
    private val repository: EmojiEffectsRepository,
    private val normalizeEmojiUseCase: NormalizeEmojiUseCase = NormalizeEmojiUseCase()
) {

    operator fun invoke(
        messageId: Int,
        rawEmoticon: String,
        animationIndex: Int,
        timestampMs: Long
    ): EmojiInteractionSession {
        val normalized = normalizeEmojiUseCase(rawEmoticon) ?: rawEmoticon
        return repository.recordTap(
            messageId = messageId,
            emoticon = normalized,
            animationIndex = animationIndex,
            timestampMs = timestampMs
        )
    }
}
