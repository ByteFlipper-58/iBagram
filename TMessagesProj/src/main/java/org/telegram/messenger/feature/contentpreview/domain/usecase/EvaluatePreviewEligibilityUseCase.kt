package org.telegram.messenger.feature.contentpreview.domain.usecase

import org.telegram.messenger.feature.contentpreview.domain.model.ContentPreviewItem
import org.telegram.messenger.feature.contentpreview.domain.model.PreviewContentType

/**
 * Юзкейс для проверки возможности открытия предварительного просмотра контента.
 */
class EvaluatePreviewEligibilityUseCase {

    operator fun invoke(item: ContentPreviewItem?): Boolean {
        if (item == null) return false
        if (item.contentType == PreviewContentType.NONE) return false
        return item.documentId != 0L || !item.emoticon.isNullOrEmpty()
    }
}
