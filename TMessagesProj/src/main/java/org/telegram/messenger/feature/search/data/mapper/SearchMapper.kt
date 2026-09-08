package org.telegram.messenger.feature.search.data.mapper

import org.telegram.messenger.ChatObject
import org.telegram.messenger.ContactsController
import org.telegram.messenger.MessageObject
import org.telegram.messenger.UserObject
import org.telegram.messenger.feature.search.domain.model.SearchResultModel
import org.telegram.messenger.feature.search.domain.model.SearchResultType
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC

object SearchMapper {

    fun mapUser(user: TLRPC.User): SearchResultModel {
        val name = ContactsController.formatName(user.first_name, user.last_name)
        val title = if (name.isNotBlank()) name else user.username ?: "User #${user.id}"
        val type = if (user.bot) SearchResultType.BOT else SearchResultType.USER

        return SearchResultModel(
            id = user.id,
            title = title,
            subtitle = if (!user.username.isNullOrEmpty()) "@${user.username}" else null,
            username = user.username,
            type = type,
            isVerified = user.verified,
            isScam = user.scam,
            isFake = user.fake,
            originalObject = user
        )
    }

    fun mapChat(chat: TLRPC.Chat): SearchResultModel {
        val isChannel = ChatObject.isChannel(chat) && !chat.megagroup
        val type = if (isChannel) SearchResultType.CHANNEL else SearchResultType.GROUP

        return SearchResultModel(
            id = -chat.id,
            title = chat.title ?: "Chat #${chat.id}",
            subtitle = if (!chat.username.isNullOrEmpty()) "@${chat.username}" else null,
            username = chat.username,
            type = type,
            isVerified = chat.verified,
            isScam = chat.scam,
            isFake = chat.fake,
            originalObject = chat
        )
    }

    fun mapMessage(messageObject: MessageObject): SearchResultModel {
        val messageText = messageObject.messageText?.toString() ?: ""
        val title = if (messageText.isNotBlank()) messageText else "Media"

        return SearchResultModel(
            id = messageObject.getId().toLong(),
            title = title,
            subtitle = null,
            type = SearchResultType.MESSAGE,
            date = messageObject.messageOwner?.date?.toLong() ?: 0L,
            chatId = messageObject.getDialogId(),
            messageId = messageObject.getId(),
            originalObject = messageObject
        )
    }

    fun mapHashtag(hashtag: String, date: Long = 0L): SearchResultModel {
        val cleanTag = if (hashtag.startsWith("#")) hashtag else "#$hashtag"
        return SearchResultModel(
            id = cleanTag.hashCode().toLong(),
            title = cleanTag,
            subtitle = null,
            type = SearchResultType.HASHTAG,
            date = date,
            originalObject = cleanTag
        )
    }

    fun mapTLObject(obj: TLObject): SearchResultModel? {
        return when (obj) {
            is TLRPC.User -> mapUser(obj)
            is TLRPC.Chat -> mapChat(obj)
            is MessageObject -> mapMessage(obj)
            else -> null
        }
    }
}
