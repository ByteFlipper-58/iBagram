package org.telegram.messenger.feature.reactions.data.mapper

import org.telegram.messenger.feature.reactions.domain.model.MessageReactionCountModel
import org.telegram.messenger.feature.reactions.domain.model.ReactionItemModel
import org.telegram.tgnet.TLRPC

object ReactionMapper {

    fun mapAvailableReaction(reaction: TLRPC.TL_availableReaction): ReactionItemModel {
        return ReactionItemModel(
            reaction = reaction.reaction ?: "",
            title = reaction.title ?: "",
            isCustom = false,
            documentId = 0L,
            isInactive = reaction.inactive,
            isPremium = reaction.premium,
            isPaid = false,
            order = reaction.positionInList
        )
    }

    fun mapReaction(reaction: TLRPC.Reaction): ReactionItemModel {
        return when (reaction) {
            is TLRPC.TL_reactionEmoji -> {
                ReactionItemModel(
                    reaction = reaction.emoticon ?: "",
                    title = reaction.emoticon ?: "",
                    isCustom = false,
                    documentId = 0L,
                    isInactive = false,
                    isPremium = false,
                    isPaid = false
                )
            }
            is TLRPC.TL_reactionCustomEmoji -> {
                ReactionItemModel(
                    reaction = reaction.document_id.toString(),
                    title = "Custom",
                    isCustom = true,
                    documentId = reaction.document_id,
                    isInactive = false,
                    isPremium = false,
                    isPaid = false
                )
            }
            is TLRPC.TL_reactionPaid -> {
                ReactionItemModel(
                    reaction = "⭐️",
                    title = "Paid",
                    isCustom = false,
                    documentId = 0L,
                    isInactive = false,
                    isPremium = false,
                    isPaid = true
                )
            }
            else -> {
                ReactionItemModel(
                    reaction = "",
                    title = "",
                    isCustom = false,
                    documentId = 0L,
                    isInactive = false,
                    isPremium = false,
                    isPaid = false
                )
            }
        }
    }

    fun toTLReaction(model: ReactionItemModel): TLRPC.Reaction {
        return if (model.isPaid) {
            TLRPC.TL_reactionPaid()
        } else if (model.isCustom && model.documentId != 0L) {
            TLRPC.TL_reactionCustomEmoji().apply {
                document_id = model.documentId
            }
        } else {
            TLRPC.TL_reactionEmoji().apply {
                emoticon = model.reaction
            }
        }
    }

    fun mapReactionCount(reactionCount: TLRPC.ReactionCount): MessageReactionCountModel {
        val model = reactionCount.reaction?.let { mapReaction(it) }
            ?: ReactionItemModel(reaction = "", title = "")
        val isChosen = if (reactionCount is TLRPC.TL_reactionCount) reactionCount.chosen else false
        val chosenOrder = if (reactionCount is TLRPC.TL_reactionCount) reactionCount.chosen_order else 0
        return MessageReactionCountModel(
            reaction = model,
            count = reactionCount.count,
            isChosen = isChosen,
            chosenOrder = chosenOrder
        )
    }
}
