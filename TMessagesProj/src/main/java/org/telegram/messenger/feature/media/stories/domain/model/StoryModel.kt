package org.telegram.messenger.feature.media.stories.domain.model

data class StoryModel(
    val id: Int,
    val dialogId: Long,
    val date: Long,
    val expireDate: Long = 0L,
    val caption: String? = null,
    val mediaPath: String? = null,
    val isPinned: Boolean = false,
    val isCloseFriends: Boolean = false,
    val isOut: Boolean = false,
    val isEdited: Boolean = false,
    val viewsCount: Int = 0,
    val reactionsCount: Int = 0,
    val forwardsCount: Int = 0,
    val isUnread: Boolean = false
)
