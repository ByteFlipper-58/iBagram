package org.telegram.messenger.feature.dialogs.data.mapper

import org.telegram.messenger.feature.dialogs.domain.model.DialogModel
import org.telegram.tgnet.TLRPC

/**
 * Maps legacy Telegram [TLRPC.Dialog] to clean [DialogModel] domain instance.
 */
object DialogMapper {

    fun mapToDomain(
        dialog: TLRPC.Dialog,
        isMuted: Boolean = false,
        isForum: Boolean = false
    ): DialogModel {
        val draftText = when (val draft = dialog.draft) {
            is TLRPC.TL_draftMessage -> draft.message
            else -> null
        }

        return DialogModel(
            id = dialog.id,
            unreadCount = dialog.unread_count,
            unreadMentionsCount = dialog.unread_mentions_count,
            unreadReactionsCount = dialog.unread_reactions_count,
            lastMessageDate = dialog.last_message_date,
            lastMessageId = dialog.top_message,
            isPinned = dialog.pinned,
            pinnedNum = dialog.pinnedNum,
            isMuted = isMuted,
            folderId = dialog.folder_id,
            isForum = isForum,
            draftText = draftText
        )
    }
}
