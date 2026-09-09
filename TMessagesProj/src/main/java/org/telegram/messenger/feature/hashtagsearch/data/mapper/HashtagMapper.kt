package org.telegram.messenger.feature.hashtagsearch.data.mapper

import org.telegram.messenger.MessageObject
import org.telegram.messenger.feature.hashtagsearch.domain.model.HashtagMessageModel
import org.telegram.messenger.feature.hashtagsearch.domain.model.HashtagSearchResultModel
import org.telegram.messenger.feature.hashtagsearch.domain.model.HashtagSearchType
import org.telegram.ui.ChatActivity

object HashtagMapper {

    fun normalizeHashtag(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        return if (!trimmed.startsWith("#") && !trimmed.startsWith("$")) {
            "#$trimmed"
        } else {
            trimmed
        }
    }

    fun mapSearchTypeToInt(type: HashtagSearchType): Int {
        return when (type) {
            HashtagSearchType.MY_MESSAGES -> ChatActivity.SEARCH_MY_MESSAGES
            HashtagSearchType.PUBLIC_POSTS -> ChatActivity.SEARCH_PUBLIC_POSTS
            HashtagSearchType.CHANNEL_POSTS -> ChatActivity.SEARCH_CHANNEL_POSTS
        }
    }

    fun mapIntToSearchType(id: Int): HashtagSearchType {
        return when (id) {
            ChatActivity.SEARCH_MY_MESSAGES -> HashtagSearchType.MY_MESSAGES
            ChatActivity.SEARCH_PUBLIC_POSTS -> HashtagSearchType.PUBLIC_POSTS
            ChatActivity.SEARCH_CHANNEL_POSTS -> HashtagSearchType.CHANNEL_POSTS
            else -> HashtagSearchType.MY_MESSAGES
        }
    }

    fun mapMessage(obj: MessageObject?): HashtagMessageModel? {
        if (obj == null) return null
        val owner = obj.messageOwner ?: return null
        return HashtagMessageModel(
            id = owner.id,
            realId = owner.realId,
            dialogId = obj.dialogId,
            text = obj.messageText?.toString() ?: "",
            date = owner.date.toLong(),
            chatTitle = null,
            isGroupPrimary = obj.isPrimaryGroupMessage
        )
    }

    fun mapMessages(list: List<MessageObject>?): List<HashtagMessageModel> {
        if (list == null) return emptyList()
        return list.mapNotNull { mapMessage(it) }
    }
}
