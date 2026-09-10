package org.telegram.messenger.feature.media.contentpreview.domain.usecase

import org.telegram.messenger.feature.media.contentpreview.domain.model.ContentPreviewItem
import org.telegram.messenger.feature.media.contentpreview.domain.repository.ContentPreviewRepository

/**
 * Юзкейс для открытия окна предпросмотра контента.
 */
class OpenContentPreviewUseCase(
    private val repository: ContentPreviewRepository,
    private val evaluatePreviewEligibilityUseCase: EvaluatePreviewEligibilityUseCase,
    private val resolvePreviewActionsUseCase: ResolvePreviewActionsUseCase
) {
    operator fun invoke(item: ContentPreviewItem): Boolean {
        if (!evaluatePreviewEligibilityUseCase(item)) {
            return false
        }
        val actions = resolvePreviewActionsUseCase(item)
        repository.openPreview(item, actions)
        return true
    }
}
