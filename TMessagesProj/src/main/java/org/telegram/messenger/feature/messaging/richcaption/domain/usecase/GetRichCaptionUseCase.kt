package org.telegram.messenger.feature.messaging.richcaption.domain.usecase

import org.telegram.messenger.feature.messaging.richcaption.domain.model.RichCaptionModel
import org.telegram.messenger.feature.messaging.richcaption.domain.repository.RichCaptionRepository

class GetRichCaptionUseCase(
    private val repository: RichCaptionRepository
) {
    operator fun invoke(): RichCaptionModel = repository.getCaption()
}
