package org.telegram.messenger.feature.richcaption.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.richcaption.domain.model.RichCaptionModel
import org.telegram.messenger.feature.richcaption.domain.repository.RichCaptionRepository

class ObserveRichCaptionUseCase(
    private val repository: RichCaptionRepository
) {
    operator fun invoke(): Flow<RichCaptionModel> = repository.observeCaption()
}
