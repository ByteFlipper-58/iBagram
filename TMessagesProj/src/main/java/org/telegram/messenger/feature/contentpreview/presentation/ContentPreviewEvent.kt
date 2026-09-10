package org.telegram.messenger.feature.contentpreview.presentation

import org.telegram.messenger.feature.contentpreview.domain.model.ContentPreviewItem
import org.telegram.messenger.feature.contentpreview.domain.model.PreviewActionType

/**
 * MVI-события управления предпросмотром контента.
 */
sealed interface ContentPreviewEvent {
    data class OnOpenPreviewRequested(val item: ContentPreviewItem) : ContentPreviewEvent
    data class OnDragUpdated(val startY: Float, val currentY: Float, val maxDragDistance: Float = 200f) : ContentPreviewEvent
    data class OnActionSelected(val action: PreviewActionType) : ContentPreviewEvent
    data object OnDismissRequested : ContentPreviewEvent
    data object OnClearRequested : ContentPreviewEvent
}
