package org.telegram.messenger.feature.messaging.botforum.data.mapper

import org.telegram.messenger.BotForumHelper
import org.telegram.messenger.feature.messaging.botforum.domain.model.BotForumDraftDeleteNotificationModel
import org.telegram.messenger.feature.messaging.botforum.domain.model.BotForumDraftUpdateNotificationModel
import org.telegram.messenger.feature.messaging.botforum.domain.model.BotForumTopicCreateNotificationModel
import org.telegram.messenger.feature.messaging.botforum.domain.model.StreamingSendButtonState

/**
 * Maps legacy BotForumHelper objects and enums to pure domain representations.
 */
object BotForumMapper {

    fun mapStreamingButtonState(state: BotForumHelper.SteamingSendButtonState?): StreamingSendButtonState {
        return when (state) {
            BotForumHelper.SteamingSendButtonState.BLOCKING -> StreamingSendButtonState.BLOCKING
            BotForumHelper.SteamingSendButtonState.STOP -> StreamingSendButtonState.STOP
            else -> StreamingSendButtonState.NO_STREAMING
        }
    }

    fun mapToLegacyStreamingButtonState(state: StreamingSendButtonState): BotForumHelper.SteamingSendButtonState {
        return when (state) {
            StreamingSendButtonState.BLOCKING -> BotForumHelper.SteamingSendButtonState.BLOCKING
            StreamingSendButtonState.STOP -> BotForumHelper.SteamingSendButtonState.STOP
            StreamingSendButtonState.NO_STREAMING -> BotForumHelper.SteamingSendButtonState.NO_STREAMING
        }
    }

    fun mapDraftUpdateNotification(
        notification: BotForumHelper.BotForumTextDraftUpdateNotification
    ): BotForumDraftUpdateNotificationModel {
        val text = notification.messageObject?.messageText?.toString()
            ?: notification.messageObject?.messageOwner?.message
            ?: ""
        val localId = notification.messageObject?.id ?: 0
        return BotForumDraftUpdateNotificationModel(
            botUserId = notification.botUserId,
            botTopicId = notification.botTopicId,
            localMessageId = localId,
            text = text,
            isNew = notification.isNew
        )
    }

    fun mapDraftDeleteNotification(
        notification: BotForumHelper.BotForumTextDraftDeleteNotification
    ): BotForumDraftDeleteNotificationModel {
        return BotForumDraftDeleteNotificationModel(
            botUserId = notification.botUserId,
            botTopicId = notification.botTopicId,
            messageId = notification.messageId
        )
    }

    fun mapTopicCreateNotification(
        notification: BotForumHelper.BotForumTopicCreateNotification
    ): BotForumTopicCreateNotificationModel {
        return BotForumTopicCreateNotificationModel(
            dialogId = notification.dialogId,
            topicId = notification.topicId,
            title = ""
        )
    }
}
