package org.telegram.messenger.feature.messaging.richcaption.domain.usecase

import org.telegram.messenger.feature.messaging.richcaption.domain.model.CaptionMeasureSpec
import org.telegram.messenger.feature.messaging.richcaption.domain.repository.RichCaptionRepository

class CalculateCaptionMeasureWidthUseCase(
    private val repository: RichCaptionRepository
) {
    operator fun invoke(spec: CaptionMeasureSpec): Int = repository.calculateAvailableWidth(spec)
}
