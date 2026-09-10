package org.telegram.messenger.feature.messaging.chatmeta.data.mapper

import org.telegram.messenger.MessageObject
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.MessageMetadataCheckItem

object ChatMetadataMapper {

    fun mapMessageObject(
        dialogId: Long,
        messageObject: MessageObject?,
        currentTime: Long = System.currentTimeMillis()
    ): MessageMetadataCheckItem? {
        if (messageObject == null || messageObject.id <= 0) return null

        val canHaveReactions = messageObject.messageOwner?.action == null || messageObject.canSetReaction()
        val hasExtendedMedia = messageObject.hasExtendedMediaPreview() || messageObject.hasPaidMediaPreview()
        
        val isStoryType = messageObject.type == MessageObject.TYPE_STORY || 
                          messageObject.type == MessageObject.TYPE_STORY_MENTION
        val hasStory = isStoryType || messageObject.messageOwner?.replyStory != null

        val storyItem = if (isStoryType) {
            messageObject.messageOwner?.media?.storyItem
        } else {
            messageObject.messageOwner?.replyStory
        }

        val storyId = storyItem?.id ?: 0
        val storyPeerId = if (isStoryType) {
            messageObject.messageOwner?.media?.user_id ?: 0L
        } else {
            0L
        }

        val storyLastUpdateTime = storyItem?.lastUpdateTime ?: 0L

        return MessageMetadataCheckItem(
            messageId = messageObject.id,
            dialogId = dialogId,
            canHaveReactions = canHaveReactions,
            hasExtendedMedia = hasExtendedMedia,
            hasStory = hasStory,
            storyId = storyId,
            storyPeerId = storyPeerId,
            lastReactionsCheckTime = messageObject.reactionsLastCheckTime,
            lastExtendedMediaCheckTime = messageObject.extendedMediaLastCheckTime,
            lastStoryCheckTime = storyLastUpdateTime
        )
    }

    fun mapMessageObjects(
        dialogId: Long,
        messageObjects: List<MessageObject>?,
        currentTime: Long = System.currentTimeMillis()
    ): List<MessageMetadataCheckItem> {
        if (messageObjects == null) return emptyList()
        val result = mutableListOf<MessageMetadataCheckItem>()
        for (obj in messageObjects) {
            val item = mapMessageObject(dialogId, obj, currentTime)
            if (item != null) {
                result.add(item)
            }
        }
        return result
    }
}
