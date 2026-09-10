package org.telegram.messenger.feature.contentpreview.presentation

import org.telegram.messenger.feature.contentpreview.domain.model.ContentPreviewItem
import org.telegram.messenger.feature.contentpreview.domain.model.PreviewActionItem
import org.telegram.messenger.feature.contentpreview.domain.model.PreviewContentType

/**
 * UI-состояние экрана предпросмотра контента.
 */
data class ContentPreviewUiState(
    val isVisible: Boolean = false,
    val isMenuVisible: Boolean = false,
    val currentItem: ContentPreviewItem? = null,
    val availableActions: List<PreviewActionItem> = emptyList(),
    val dragProgress: Float = 0f
) {
    val contentType: PreviewContentType get() = currentItem?.contentType ?: PreviewContentType.NONE
    val hasActions: Boolean get() = availableActions.isNotEmpty()
    val canSend: Boolean get() = currentItem?.canSend ?: false
}
