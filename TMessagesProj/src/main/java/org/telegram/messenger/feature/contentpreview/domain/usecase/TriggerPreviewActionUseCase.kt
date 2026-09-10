package org.telegram.messenger.feature.contentpreview.domain.usecase

import org.telegram.messenger.feature.contentpreview.domain.model.ContentPreviewItem
import org.telegram.messenger.feature.contentpreview.domain.model.PreviewActionType
import org.telegram.messenger.feature.contentpreview.domain.repository.ContentPreviewRepository

/**
 * Юзкейс для обработки выбора действия в контекстном меню превью.
 */
class TriggerPreviewActionUseCase(
    private val repository: ContentPreviewRepository
) {
    operator fun invoke(action: PreviewActionType, item: ContentPreviewItem?): Boolean {
        if (item == null) return false
        // После выполнения действия окно предпросмотра закрывается
        repository.dismissPreview()
        return true
    }
}
