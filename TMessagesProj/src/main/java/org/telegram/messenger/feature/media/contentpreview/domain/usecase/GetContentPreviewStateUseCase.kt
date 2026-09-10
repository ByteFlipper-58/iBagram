package org.telegram.messenger.feature.media.contentpreview.domain.usecase

import org.telegram.messenger.feature.media.contentpreview.domain.model.ContentPreviewState
import org.telegram.messenger.feature.media.contentpreview.domain.repository.ContentPreviewRepository

/**
 * Юзкейс для получения текущего состояния предпросмотра контента.
 */
class GetContentPreviewStateUseCase(
    private val repository: ContentPreviewRepository
) {
    operator fun invoke(): ContentPreviewState {
        return repository.getState()
    }
}
