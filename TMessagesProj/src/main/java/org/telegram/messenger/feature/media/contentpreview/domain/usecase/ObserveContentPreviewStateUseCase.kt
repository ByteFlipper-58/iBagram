package org.telegram.messenger.feature.media.contentpreview.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.contentpreview.domain.model.ContentPreviewState
import org.telegram.messenger.feature.media.contentpreview.domain.repository.ContentPreviewRepository

/**
 * Юзкейс для наблюдения за состоянием предпросмотра контента.
 */
class ObserveContentPreviewStateUseCase(
    private val repository: ContentPreviewRepository
) {
    operator fun invoke(): Flow<ContentPreviewState> {
        return repository.observeState()
    }
}
