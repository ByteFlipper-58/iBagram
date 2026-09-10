package org.telegram.messenger.feature.richcaption.domain.usecase

import org.telegram.messenger.feature.richcaption.domain.repository.RichCaptionRepository

class SetRichCaptionCreditUseCase(
    private val repository: RichCaptionRepository
) {
    operator fun invoke(credit: String?) {
        repository.setCaptionCredit(credit)
    }
}
