package org.telegram.messenger.feature.sharedmedia.presentation

import org.telegram.messenger.feature.sharedmedia.domain.model.SharedMediaFilterType
import org.telegram.messenger.feature.sharedmedia.domain.model.SharedMediaItem
import org.telegram.messenger.feature.sharedmedia.domain.model.SharedMediaTabSpec
import org.telegram.messenger.feature.sharedmedia.domain.model.SharedMediaTabType

/**
 * MVI-события для управления общим медиа.
 */
sealed interface SharedMediaEvent {
    data class OnDialogConfigured(val dialogId: Long, val isEncrypted: Boolean) : SharedMediaEvent
    data class OnAvailableTabsUpdated(val tabs: List<SharedMediaTabSpec>) : SharedMediaEvent
    data class OnTabSelected(val tab: SharedMediaTabType) : SharedMediaEvent
    data class OnFilterChanged(val filter: SharedMediaFilterType) : SharedMediaEvent
    data class OnItemsLoaded(val items: List<SharedMediaItem>, val hasMore: Boolean) : SharedMediaEvent
    data class OnMoreItemsLoaded(val items: List<SharedMediaItem>, val hasMore: Boolean) : SharedMediaEvent
    data class OnItemSelectionToggled(val messageId: Int) : SharedMediaEvent
    data object OnSelectAllRequested : SharedMediaEvent
    data object OnClearSelectionRequested : SharedMediaEvent
    data object OnDeleteSelectedRequested : SharedMediaEvent
    data object OnClearRequested : SharedMediaEvent
}
