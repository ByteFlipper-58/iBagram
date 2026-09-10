package org.telegram.messenger.feature.sharedmedia.presentation

import org.telegram.messenger.feature.sharedmedia.domain.model.SharedMediaFilterType
import org.telegram.messenger.feature.sharedmedia.domain.model.SharedMediaItem
import org.telegram.messenger.feature.sharedmedia.domain.model.SharedMediaPeriod
import org.telegram.messenger.feature.sharedmedia.domain.model.SharedMediaSelectionState
import org.telegram.messenger.feature.sharedmedia.domain.model.SharedMediaTabSpec
import org.telegram.messenger.feature.sharedmedia.domain.model.SharedMediaTabType

/**
 * UI-состояние для экрана общего медиа.
 */
data class SharedMediaUiState(
    val dialogId: Long = 0L,
    val isEncrypted: Boolean = false,
    val currentTab: SharedMediaTabType = SharedMediaTabType.PHOTO_VIDEO,
    val filterType: SharedMediaFilterType = SharedMediaFilterType.ALL,
    val availableTabs: List<SharedMediaTabSpec> = emptyList(),
    val items: List<SharedMediaItem> = emptyList(),
    val sections: Map<String, List<SharedMediaItem>> = emptyMap(),
    val periods: List<SharedMediaPeriod> = emptyList(),
    val selection: SharedMediaSelectionState = SharedMediaSelectionState(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true
) {
    val isSelectionActive: Boolean get() = selection.isSelectionActive
    val selectedCount: Int get() = selection.count
    val canForward: Boolean get() = selection.canForward
    val canDelete: Boolean get() = selection.canDelete
    val canPin: Boolean get() = selection.canPin
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
}
