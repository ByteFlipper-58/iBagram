package org.telegram.messenger.feature.messaging.chatattach.domain.usecase

import org.telegram.messenger.feature.messaging.chatattach.domain.model.CaptionLimitInfo

class CalculateAttachCaptionLimitUseCase {
    companion object {
        const val STANDARD_CAPTION_LIMIT = 1024
        const val PREMIUM_CAPTION_LIMIT = 2048
    }

    operator fun invoke(caption: String, isPremium: Boolean): CaptionLimitInfo {
        val maxLimit = if (isPremium) PREMIUM_CAPTION_LIMIT else STANDARD_CAPTION_LIMIT
        val codePoints = caption.codePointCount(0, caption.length)
        val remaining = maxLimit - codePoints
        return CaptionLimitInfo(
            maxLimit = maxLimit,
            currentLength = codePoints,
            remainingCharacters = remaining,
            isLimitExceeded = remaining < 0
        )
    }
}
