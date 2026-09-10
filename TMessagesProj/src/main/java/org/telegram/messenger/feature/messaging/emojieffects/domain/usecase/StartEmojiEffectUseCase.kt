package org.telegram.messenger.feature.messaging.emojieffects.domain.usecase

import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiEffectItem
import org.telegram.messenger.feature.messaging.emojieffects.domain.repository.EmojiEffectsRepository

class StartEmojiEffectUseCase(
    private val repository: EmojiEffectsRepository,
    private val evaluateAnimationQuotaUseCase: EvaluateAnimationQuotaUseCase = EvaluateAnimationQuotaUseCase()
) {
    operator fun invoke(item: EmojiEffectItem): Boolean {
        val state = repository.getState()
        val countForMsg = state.activeEffects.count { it.messageId == item.messageId }
        val quota = evaluateAnimationQuotaUseCase(
            currentGlobalCount = state.activeEffects.size,
            currentMessageCount = countForMsg
        )
        if (!quota.isAllowed) return false

        repository.startEffect(item)
        if (item.documentId != 0L) {
            val lastIdx = repository.getLastAnimationIndex(item.documentId)
            repository.updateLastAnimationIndex(item.documentId, (lastIdx + 1) % 4)
        }
        return true
    }
}
