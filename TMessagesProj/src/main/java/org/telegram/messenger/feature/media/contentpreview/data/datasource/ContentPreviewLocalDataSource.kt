package org.telegram.messenger.feature.media.contentpreview.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.media.contentpreview.domain.model.ContentPreviewItem
import org.telegram.messenger.feature.media.contentpreview.domain.model.ContentPreviewState
import org.telegram.messenger.feature.media.contentpreview.domain.model.PreviewActionItem

/**
 * Local data source managing interactive content preview overlay state,
 * touch drag progress, and contextual actions.
 */
class ContentPreviewLocalDataSource(
    var testMode: Boolean = false
) {

    private val lock = Any()
    private val _state = MutableStateFlow(ContentPreviewState())

    fun observeState(): Flow<ContentPreviewState> = _state.asStateFlow()

    fun getState(): ContentPreviewState = synchronized(lock) { _state.value }

    fun openPreview(item: ContentPreviewItem, actions: List<PreviewActionItem>) {
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

    fun updateDragProgress(dragProgress: Float, isMenuVisible: Boolean) {
        synchronized(lock) {
            if (!_state.value.isVisible) return
            _state.value = _state.value.copy(
                dragProgress = dragProgress,
                isMenuVisible = isMenuVisible
            )
        }
    }

    fun dismissPreview() {
        synchronized(lock) {
            _state.value = ContentPreviewState()
        }
    }

    fun clear() {
        synchronized(lock) {
            _state.value = ContentPreviewState()
        }
    }
}
