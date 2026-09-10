package org.telegram.messenger.feature.messaging.richcaption.domain.usecase

import org.telegram.messenger.feature.messaging.richcaption.domain.model.CaptionEntitySpan
import org.telegram.messenger.feature.messaging.richcaption.domain.repository.RichCaptionRepository

class SetRichCaptionTextUseCase(
    private val repository: RichCaptionRepository
) {
    operator fun invoke(text: String, spans: List<CaptionEntitySpan> = emptyList()) {
        repository.setCaptionText(text, spans)
    }
}
