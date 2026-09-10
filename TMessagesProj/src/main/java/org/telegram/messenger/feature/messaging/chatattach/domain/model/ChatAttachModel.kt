package org.telegram.messenger.feature.messaging.chatattach.domain.model

enum class ChatAttachLayoutType {
    PHOTO,
    MUSIC,
    DOCUMENTS,
    CONTACTS,
    LOCATION,
    POLL,
    REPLIES,
    TODO,
    STICKERS,
    EMOJI,
    LINK,
    RICH
}

data class ChatAttachItem(
    val id: String,
    val type: ChatAttachLayoutType,
    val path: String? = null,
    val displayName: String? = null,
    val size: Long = 0L,
    val duration: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val isVideo: Boolean = false,
    val hasSpoiler: Boolean = false,
    val order: Int = 0
)

data class ChatAttachSendOptions(
    val sendAsFile: Boolean = false,
    val caption: String = "",
    val isCaptionAbove: Boolean = false,
    val hasSpoiler: Boolean = false,
    val ttlSeconds: Int = 0,
    val scheduleDate: Int = 0,
    val notify: Boolean = true,
    val starsPrice: Long = 0L,
    val groupAsAlbum: Boolean = true
)

data class ChatAttachPermissions(
    val canSendPhotos: Boolean = true,
    val canSendVideos: Boolean = true,
    val canSendMusic: Boolean = true,
    val canSendDocuments: Boolean = true,
    val canSendLocation: Boolean = true,
    val canSendContacts: Boolean = true,
    val canSendPolls: Boolean = true,
    val isRestricted: Boolean = false,
    val restrictionReason: String? = null
)

data class CaptionLimitInfo(
    val maxLimit: Int,
    val currentLength: Int,
    val remainingCharacters: Int,
    val isLimitExceeded: Boolean
)

data class ChatAttachState(
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
    val isAlertVisible: Boolean = false,
    val maxSelectionLimit: Int = 100
)
