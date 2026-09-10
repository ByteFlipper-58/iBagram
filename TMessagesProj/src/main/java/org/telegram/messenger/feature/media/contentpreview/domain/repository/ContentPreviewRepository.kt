package org.telegram.messenger.feature.media.contentpreview.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.contentpreview.domain.model.ContentPreviewItem
import org.telegram.messenger.feature.media.contentpreview.domain.model.ContentPreviewState
import org.telegram.messenger.feature.media.contentpreview.domain.model.PreviewActionItem

/**
 * Интерфейс репозитория управления предпросмотром контента (стикеров, эмодзи, GIF).
 */
interface ContentPreviewRepository {
    fun observeState(): Flow<ContentPreviewState>
    fun getState(): ContentPreviewState
    fun openPreview(item: ContentPreviewItem, actions: List<PreviewActionItem>)
    fun updateDragProgress(dragProgress: Float, isMenuVisible: Boolean)
    fun dismissPreview()
    fun clear()
}
