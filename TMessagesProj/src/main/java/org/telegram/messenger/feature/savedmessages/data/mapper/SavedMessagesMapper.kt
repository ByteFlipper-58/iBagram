package org.telegram.messenger.feature.savedmessages.data.mapper

import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.messenger.SavedMessagesController
import org.telegram.messenger.UserConfig
import org.telegram.messenger.UserObject
import org.telegram.messenger.feature.savedmessages.domain.model.SavedDialogModel

/**
 * Maps legacy Telegram [SavedMessagesController.SavedDialog] objects to clean [SavedDialogModel] instances.
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
