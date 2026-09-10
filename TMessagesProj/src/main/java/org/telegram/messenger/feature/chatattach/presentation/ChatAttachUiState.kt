package org.telegram.messenger.feature.chatattach.presentation

import org.telegram.messenger.feature.chatattach.domain.model.CaptionLimitInfo
import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachItem
import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachLayoutType
import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachPermissions
import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachSendOptions

data class ChatAttachUiState(
    val currentLayout: ChatAttachLayoutType = ChatAttachLayoutType.PHOTO,
    val availableLayouts: List<ChatAttachLayoutType> = listOf(
        ChatAttachLayoutType.PHOTO,
        ChatAttachLayoutType.DOCUMENTS,
        ChatAttachLayoutType.LOCATION,
        ChatAttachLayoutType.CONTACTS,
        ChatAttachLayoutType.MUSIC,
        ChatAttachLayoutType.POLL
    ),
    val selectedItems: List<ChatAttachItem> = emptyList(),
    val sendOptions: ChatAttachSendOptions = ChatAttachSendOptions(),
    val permissions: ChatAttachPermissions = ChatAttachPermissions(),
    val captionInfo: CaptionLimitInfo = CaptionLimitInfo(1024, 0, 1024, false),
    val isAlertVisible: Boolean = false,
    val isSendEnabled: Boolean = false
) {
    val selectedCount: Int get() = selectedItems.size
    val hasMultipleSelection: Boolean get() = selectedItems.size > 1
    val isPhotoLayout: Boolean get() = currentLayout == ChatAttachLayoutType.PHOTO
    val isDocumentLayout: Boolean get() = currentLayout == ChatAttachLayoutType.DOCUMENTS
}
