package org.telegram.messenger.feature.contentpreview.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.contentpreview.domain.model.ContentPreviewItem
import org.telegram.messenger.feature.contentpreview.domain.model.ContentPreviewState
import org.telegram.messenger.feature.contentpreview.domain.model.PreviewActionItem
import org.telegram.messenger.feature.contentpreview.domain.repository.ContentPreviewRepository

/**
 * Потокобезопасная реализация ContentPreviewRepository.
 */
class LegacyContentPreviewRepository : ContentPreviewRepository {

    private val lock = Any()
    private val _state = MutableStateFlow(ContentPreviewState())

    override fun observeState(): Flow<ContentPreviewState> = _state.asStateFlow()

    override fun getState(): ContentPreviewState = synchronized(lock) { _state.value }

    override fun openPreview(item: ContentPreviewItem, actions: List<PreviewActionItem>) {
        synchronized(lock) {
            _state.value = ContentPreviewState(
                isVisible = true,
                isMenuVisible = false,
                currentItem = item,
                availableActions = actions,
                dragProgress = 0f
            )
        }
    }

    override fun updateDragProgress(dragProgress: Float, isMenuVisible: Boolean) {
        synchronized(lock) {
            if (!_state.value.isVisible) return
            _state.value = _state.value.copy(
                dragProgress = dragProgress,
                isMenuVisible = isMenuVisible
            )
        }
    }

    override fun dismissPreview() {
        synchronized(lock) {
            _state.value = ContentPreviewState()
        }
    }

    override fun clear() {
        synchronized(lock) {
            _state.value = ContentPreviewState()
        }
    }
}
