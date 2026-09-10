package org.telegram.messenger.feature.chatattach.data.mapper

import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachLayoutType

object ChatAttachMapper {
    const val LAYOUT_TYPE_PHOTO = 1
    const val LAYOUT_TYPE_MUSIC = 3
    const val LAYOUT_TYPE_DOCUMENTS = 4
    const val LAYOUT_TYPE_CONTACTS = 5
    const val LAYOUT_TYPE_LOCATION = 6
    const val LAYOUT_TYPE_POLL = 9
    const val LAYOUT_TYPE_REPLIES = 11
    const val LAYOUT_TYPE_TODO = 12
    const val LAYOUT_TYPE_STICKERS = 13
    const val LAYOUT_TYPE_EMOJI = 14
    const val LAYOUT_TYPE_LINK = 15
    const val LAYOUT_TYPE_RICH = 16

    fun mapIntToLayoutType(type: Int): ChatAttachLayoutType {
        return when (type) {
            LAYOUT_TYPE_PHOTO -> ChatAttachLayoutType.PHOTO
            LAYOUT_TYPE_MUSIC -> ChatAttachLayoutType.MUSIC
            LAYOUT_TYPE_DOCUMENTS -> ChatAttachLayoutType.DOCUMENTS
            LAYOUT_TYPE_CONTACTS -> ChatAttachLayoutType.CONTACTS
            LAYOUT_TYPE_LOCATION -> ChatAttachLayoutType.LOCATION
            LAYOUT_TYPE_POLL -> ChatAttachLayoutType.POLL
            LAYOUT_TYPE_REPLIES -> ChatAttachLayoutType.REPLIES
            LAYOUT_TYPE_TODO -> ChatAttachLayoutType.TODO
            LAYOUT_TYPE_STICKERS -> ChatAttachLayoutType.STICKERS
            LAYOUT_TYPE_EMOJI -> ChatAttachLayoutType.EMOJI
            LAYOUT_TYPE_LINK -> ChatAttachLayoutType.LINK
            LAYOUT_TYPE_RICH -> ChatAttachLayoutType.RICH
            else -> ChatAttachLayoutType.PHOTO
        }
    }

    fun mapLayoutTypeToInt(layout: ChatAttachLayoutType): Int {
        return when (layout) {
            ChatAttachLayoutType.PHOTO -> LAYOUT_TYPE_PHOTO
            ChatAttachLayoutType.MUSIC -> LAYOUT_TYPE_MUSIC
            ChatAttachLayoutType.DOCUMENTS -> LAYOUT_TYPE_DOCUMENTS
            ChatAttachLayoutType.CONTACTS -> LAYOUT_TYPE_CONTACTS
            ChatAttachLayoutType.LOCATION -> LAYOUT_TYPE_LOCATION
            ChatAttachLayoutType.POLL -> LAYOUT_TYPE_POLL
            ChatAttachLayoutType.REPLIES -> LAYOUT_TYPE_REPLIES
            ChatAttachLayoutType.TODO -> LAYOUT_TYPE_TODO
            ChatAttachLayoutType.STICKERS -> LAYOUT_TYPE_STICKERS
            ChatAttachLayoutType.EMOJI -> LAYOUT_TYPE_EMOJI
            ChatAttachLayoutType.LINK -> LAYOUT_TYPE_LINK
            ChatAttachLayoutType.RICH -> LAYOUT_TYPE_RICH
        }
    }
}
