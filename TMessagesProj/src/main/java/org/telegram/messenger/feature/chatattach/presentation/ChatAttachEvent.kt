package org.telegram.messenger.feature.chatattach.presentation

import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachItem
import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachLayoutType
import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachPermissions
import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachSendOptions

sealed interface ChatAttachEvent {
    data class OnOpenAlert(
        val permissions: ChatAttachPermissions,
        val initialLayout: ChatAttachLayoutType = ChatAttachLayoutType.PHOTO
    ) : ChatAttachEvent

    data class OnLayoutSelected(val layout: ChatAttachLayoutType) : ChatAttachEvent
    data class OnItemSelectionToggled(val item: ChatAttachItem) : ChatAttachEvent
    data class OnItemsSelected(val items: List<ChatAttachItem>) : ChatAttachEvent
    data object OnClearSelectionRequested : ChatAttachEvent
    data class OnSendOptionsChanged(val options: ChatAttachSendOptions) : ChatAttachEvent
    data class OnCaptionChanged(val caption: String) : ChatAttachEvent
    data class OnSendAsFileToggled(val sendAsFile: Boolean) : ChatAttachEvent
    data class OnSpoilerToggled(val hasSpoiler: Boolean) : ChatAttachEvent
    data class OnCaptionAboveToggled(val isAbove: Boolean) : ChatAttachEvent
    data class OnStarsPriceChanged(val price: Long) : ChatAttachEvent
    data object OnDismissRequested : ChatAttachEvent
    data object OnClearRequested : ChatAttachEvent
}
