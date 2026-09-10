package org.telegram.messenger.feature.contentpreview.domain.usecase

import org.telegram.messenger.feature.contentpreview.domain.repository.ContentPreviewRepository

/**
 * Юзкейс для сброса состояния превью контента.
 */
class ClearContentPreviewUseCase(
    private val repository: ContentPreviewRepository
) {
    operator fun invoke() {
        repository.clear()
    }
}
