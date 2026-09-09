package org.telegram.messenger.feature.hashtagsearch.domain.model

enum class HashtagSearchType(val id: Int) {
    MY_MESSAGES(1),
    PUBLIC_POSTS(2),
    CHANNEL_POSTS(3);

    companion object {
        fun fromId(id: Int): HashtagSearchType {
            return values().firstOrNull { it.id == id } ?: MY_MESSAGES
        }
    }
}
