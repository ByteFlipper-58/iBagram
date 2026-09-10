package org.telegram.messenger.feature.richcaption.domain.usecase

import org.telegram.messenger.feature.richcaption.domain.repository.RichCaptionRepository

class SetRichCaptionLockedUseCase(
    private val repository: RichCaptionRepository
) {
    operator fun invoke(locked: Boolean) {
        repository.setLocked(locked)
    }
}
