package org.telegram.messenger.feature.messaging.richcaption.domain.usecase

import org.telegram.messenger.feature.messaging.richcaption.domain.repository.RichCaptionRepository

class SetRichCaptionCreditUseCase(
    private val repository: RichCaptionRepository
) {
    operator fun invoke(credit: String?) {
        repository.setCaptionCredit(credit)
    }
}
