package org.telegram.messenger.feature.messaging.savedmessages.data.mapper

import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.messenger.SavedMessagesController
import org.telegram.messenger.UserConfig
import org.telegram.messenger.UserObject
import org.telegram.messenger.feature.messaging.savedmessages.domain.model.SavedDialogModel
import org.telegram.messenger.feature.messaging.savedmessages.domain.model.SavedTagModel
import org.telegram.tgnet.TLRPC

/**
 * Maps legacy Telegram [SavedMessagesController.SavedDialog] and [TLRPC.TL_savedReactionTag]
 * objects to clean [SavedDialogModel] and [SavedTagModel] domain instances.
 */
object SavedMessagesMapper {

    fun mapToDomain(account: Int, legacyDialog: SavedMessagesController.SavedDialog): SavedDialogModel {
        val title = resolveTitle(account, legacyDialog.dialogId)
        return mapToDomain(legacyDialog, title)
    }

    fun mapToDomain(
        legacyDialog: SavedMessagesController.SavedDialog,
        resolvedTitle: String,
        snippet: String? = null
    ): SavedDialogModel {
        val resolvedSnippet = snippet
            ?: legacyDialog.message?.messageText?.toString()
            ?: legacyDialog.message?.messageOwner?.message

        return SavedDialogModel(
            dialogId = legacyDialog.dialogId,
            title = resolvedTitle,
            isPinned = legacyDialog.pinned,
            unreadCount = legacyDialog.unreadCount,
            messagesCount = legacyDialog.messagesCount,
            lastMessageDate = legacyDialog.date,
            topMessageId = legacyDialog.top_message_id,
            topMessageSnippet = resolvedSnippet
        )
    }

    fun mapTagToDomain(tag: TLRPC.TL_savedReactionTag): SavedTagModel {
        val reactionStr = when (val reaction = tag.reaction) {
            is TLRPC.TL_reactionEmoji -> reaction.emoticon ?: ""
            is TLRPC.TL_reactionCustomEmoji -> reaction.document_id.toString()
            else -> tag.reaction?.toString() ?: ""
        }
        return SavedTagModel(
            reaction = reactionStr,
            title = tag.title ?: "",
            count = tag.count
        )
    }

    private fun resolveTitle(account: Int, dialogId: Long): String {
        val selfId = UserConfig.getInstance(account).clientUserId
        return when {
            dialogId == UserObject.ANONYMOUS -> {
                LocaleController.getString(R.string.AnonymousForward)
            }
            dialogId == selfId -> {
                LocaleController.getString(R.string.MyNotes)
            }
            dialogId >= 0 -> {
                val user = MessagesController.getInstance(account).getUser(dialogId)
                UserObject.getUserName(user) ?: ""
            }
            else -> {
                val chat = MessagesController.getInstance(account).getChat(-dialogId)
                chat?.title ?: ""
            }
        }
    }
}
