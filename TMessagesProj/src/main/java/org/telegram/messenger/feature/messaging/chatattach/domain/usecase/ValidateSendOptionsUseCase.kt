package org.telegram.messenger.feature.messaging.chatattach.domain.usecase

import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachSendOptions

class ValidateSendOptionsUseCase {
    fun isValid(options: ChatAttachSendOptions, isPremium: Boolean): Boolean {
        val maxLimit = if (isPremium) 2048 else 1024
        val codePoints = options.caption.codePointCount(0, options.caption.length)
        if (codePoints > maxLimit) return false
        if (options.starsPrice < 0) return false
        if (options.ttlSeconds < 0) return false
        return true
    }
}
