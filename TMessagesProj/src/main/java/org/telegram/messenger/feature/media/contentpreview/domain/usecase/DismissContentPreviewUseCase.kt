package org.telegram.messenger.feature.media.contentpreview.domain.usecase

import org.telegram.messenger.feature.media.contentpreview.domain.repository.ContentPreviewRepository

/**
 * Юзкейс для закрытия предпросмотра контента.
 */
class DismissContentPreviewUseCase(
    private val repository: ContentPreviewRepository
) {
    operator fun invoke() {
        repository.dismissPreview()
    }
}
