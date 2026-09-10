package org.telegram.messenger.feature.business.quickreplies.data.mapper

import org.telegram.messenger.feature.business.quickreplies.domain.model.QuickReplyModel
import org.telegram.ui.Business.QuickRepliesController

object QuickReplyMapper {

    fun mapToQuickReply(reply: QuickRepliesController.QuickReply): QuickReplyModel {
        return QuickReplyModel(
            id = reply.id,
            name = reply.name ?: "",
            order = reply.order,
            topMessageId = reply.topMessageId,
            messagesCount = reply.messagesCount,
            isSpecial = reply.isSpecial
        )
    }

    fun mapToQuickReplyList(replies: List<QuickRepliesController.QuickReply>): List<QuickReplyModel> {
        return replies.map { mapToQuickReply(it) }
    }
}
