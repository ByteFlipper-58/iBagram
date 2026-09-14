package org.telegram.messenger.feature.media.contentpreview.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.contentpreview.data.datasource.ContentPreviewLocalDataSource
import org.telegram.messenger.feature.media.contentpreview.data.datasource.ContentPreviewRemoteDataSource
import org.telegram.messenger.feature.media.contentpreview.domain.model.ContentPreviewItem
import org.telegram.messenger.feature.media.contentpreview.domain.model.ContentPreviewState
import org.telegram.messenger.feature.media.contentpreview.domain.model.PreviewActionItem
import org.telegram.messenger.feature.media.contentpreview.domain.repository.ContentPreviewRepository

/**
 * Clean repository coordinating content preview state,
 * gesture dragging, and remote policy validation.
 */
class ContentPreviewRepositoryImpl(
    private val localDataSource: ContentPreviewLocalDataSource,
    private val remoteDataSource: ContentPreviewRemoteDataSource
) : ContentPreviewRepository {

    override fun observeState(): Flow<ContentPreviewState> {
        return localDataSource.observeState()
    }

    override fun getState(): ContentPreviewState {
        return localDataSource.getState()
    }

    override fun openPreview(item: ContentPreviewItem, actions: List<PreviewActionItem>) {
        localDataSource.openPreview(item, actions)
    }

    override fun updateDragProgress(dragProgress: Float, isMenuVisible: Boolean) {
        localDataSource.updateDragProgress(dragProgress, isMenuVisible)
    }

    override fun dismissPreview() {
        localDataSource.dismissPreview()
    }

    override fun clear() {
        localDataSource.clear()
    }
}
