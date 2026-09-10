package org.telegram.messenger.feature.messaging.emojieffects.domain.usecase

import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiAnimationQuotaResult
import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiAnimationQuotaStatus

/**
 * Оценивает возможность запуска анимации с учетом глобальных и локальных лимитов Telegram.
 */
class EvaluateAnimationQuotaUseCase {

    companion object {
        const val MAX_GLOBAL_EFFECTS = 12
        const val MAX_PER_MESSAGE = 4
    }

    operator fun invoke(
        currentGlobalCount: Int,
        currentMessageCount: Int,
        isCacheGenerating: Boolean = false
    ): EmojiAnimationQuotaResult {
        if (currentGlobalCount >= MAX_GLOBAL_EFFECTS) {
            return EmojiAnimationQuotaResult(EmojiAnimationQuotaStatus.EXCEEDED_GLOBAL_LIMIT)
        }
        if (currentMessageCount >= MAX_PER_MESSAGE) {
            return EmojiAnimationQuotaResult(EmojiAnimationQuotaStatus.EXCEEDED_MESSAGE_LIMIT)
        }
        if (isCacheGenerating) {
            return EmojiAnimationQuotaResult(EmojiAnimationQuotaStatus.CACHE_GENERATING)
        }
        return EmojiAnimationQuotaResult(EmojiAnimationQuotaStatus.ALLOWED)
    }
}
