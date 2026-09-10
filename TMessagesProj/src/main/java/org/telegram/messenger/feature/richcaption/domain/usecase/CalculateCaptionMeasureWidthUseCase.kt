package org.telegram.messenger.feature.richcaption.domain.usecase

import org.telegram.messenger.feature.richcaption.domain.model.CaptionMeasureSpec
import org.telegram.messenger.feature.richcaption.domain.repository.RichCaptionRepository

class CalculateCaptionMeasureWidthUseCase(
    private val repository: RichCaptionRepository
) {
    operator fun invoke(spec: CaptionMeasureSpec): Int = repository.calculateAvailableWidth(spec)
}
