package org.telegram.messenger.feature.media.stories.data.mapper

import org.telegram.messenger.DialogObject
import org.telegram.messenger.feature.media.stories.domain.model.PeerStoriesModel
import org.telegram.messenger.feature.media.stories.domain.model.StealthModeModel
import org.telegram.messenger.feature.media.stories.domain.model.StoryModel
import org.telegram.tgnet.tl.TL_stories

object StoryMapper {

    fun mapStoryItem(
        item: TL_stories.StoryItem,
        fallbackDialogId: Long = 0L,
        maxReadId: Int = 0
    ): StoryModel {
        val dialogId = when {
            item.dialogId != 0L -> item.dialogId
            item.from_id != null -> DialogObject.getPeerDialogId(item.from_id)
            else -> fallbackDialogId
        }

        val viewsCount = item.views?.views_count ?: 0
        val reactionsCount = item.views?.reactions_count ?: 0
        val forwardsCount = item.views?.forwards_count ?: 0
        val isUnread = !item.out && item.id > maxReadId

        return StoryModel(
            id = item.id,
            dialogId = dialogId,
            date = item.date.toLong(),
            expireDate = item.expire_date.toLong(),
            caption = item.caption,
            mediaPath = item.attachPath ?: item.firstFramePath,
            isPinned = item.pinned,
            isCloseFriends = item.close_friends,
            isOut = item.out,
            isEdited = item.edited,
            viewsCount = viewsCount,
            reactionsCount = reactionsCount,
            forwardsCount = forwardsCount,
            isUnread = isUnread
        )
    }

    fun mapPeerStories(peerStories: TL_stories.PeerStories): PeerStoriesModel {
        val dialogId = DialogObject.getPeerDialogId(peerStories.peer)
        val maxReadId = peerStories.max_read_id
        val storiesList = peerStories.stories ?: emptyList<TL_stories.StoryItem>()
        val mappedStories = storiesList.map { mapStoryItem(it, dialogId, maxReadId) }
        val lastDate = mappedStories.maxOfOrNull { it.date } ?: 0L
        val hasUnread = mappedStories.any { it.isUnread }

        return PeerStoriesModel(
            dialogId = dialogId,
            maxReadId = maxReadId,
            stories = mappedStories,
            hasUnread = hasUnread,
            lastStoryDate = lastDate
        )
    }

    fun mapStealthMode(
        stealthMode: TL_stories.TL_storiesStealthMode?,
        currentTime: Int = (System.currentTimeMillis() / 1000).toInt()
    ): StealthModeModel {
        if (stealthMode == null) {
            return StealthModeModel(
                activeUntilDate = 0L,
                cooldownUntilDate = 0L,
                isActive = false,
                canActivateFuture = true
            )
        }
        val activeUntil = stealthMode.active_until_date
        val cooldownUntil = stealthMode.cooldown_until_date
        val isActive = activeUntil > currentTime
        val canActivateFuture = cooldownUntil <= currentTime

        return StealthModeModel(
            activeUntilDate = activeUntil.toLong(),
            cooldownUntilDate = cooldownUntil.toLong(),
            isActive = isActive,
            canActivateFuture = canActivateFuture
        )
    }
}
