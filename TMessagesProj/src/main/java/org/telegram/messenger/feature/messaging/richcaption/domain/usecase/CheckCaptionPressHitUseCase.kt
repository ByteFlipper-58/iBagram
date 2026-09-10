package org.telegram.messenger.feature.messaging.richcaption.domain.usecase

import org.telegram.messenger.feature.messaging.richcaption.domain.repository.RichCaptionRepository

class CheckCaptionPressHitUseCase(
    private val repository: RichCaptionRepository
) {
    operator fun invoke(
        localX: Int,
        localY: Int,
        textLeft: Int,
        textTop: Int,
        textWidth: Int,
        textHeight: Int
    ): Boolean = repository.isPressWithinBounds(localX, localY, textLeft, textTop, textWidth, textHeight)
}
