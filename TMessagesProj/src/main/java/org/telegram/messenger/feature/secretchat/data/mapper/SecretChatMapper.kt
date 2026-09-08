package org.telegram.messenger.feature.secretchat.data.mapper

import org.telegram.messenger.ContactsController
import org.telegram.messenger.DialogObject
import org.telegram.messenger.UserObject
import org.telegram.messenger.feature.secretchat.domain.model.SecretChatModel
import org.telegram.messenger.feature.secretchat.domain.model.SecretChatState
import org.telegram.tgnet.TLRPC

/**
 * Maps legacy Telegram EncryptedChat TL models to clean domain SecretChatModel.
 */
object SecretChatMapper {

    fun mapState(chat: TLRPC.EncryptedChat?): SecretChatState {
        return when (chat) {
            is TLRPC.TL_encryptedChatWaiting -> SecretChatState.WAITING
            is TLRPC.TL_encryptedChatRequested,
            is TLRPC.TL_encryptedChatRequested_old -> SecretChatState.REQUESTED
            is TLRPC.TL_encryptedChat,
            is TLRPC.TL_encryptedChat_old -> SecretChatState.ACTIVE
            is TLRPC.TL_encryptedChatDiscarded -> SecretChatState.DISCARDED
            null -> SecretChatState.UNKNOWN
            else -> SecretChatState.UNKNOWN
        }
    }

    fun toDomain(chat: TLRPC.EncryptedChat?, participantUser: TLRPC.User?, currentUserId: Long = 0L): SecretChatModel? {
        if (chat == null) return null

        val dialogId = DialogObject.makeEncryptedDialogId(chat.id.toLong())
        val userId = if (chat.user_id != 0L) {
            chat.user_id
        } else if (participantUser != null) {
            participantUser.id
        } else if (chat.admin_id != currentUserId && chat.admin_id != 0L) {
            chat.admin_id
        } else {
            chat.participant_id
        }

        val userName = if (participantUser != null) {
            val formatted = ContactsController.formatName(participantUser.first_name, participantUser.last_name)
            if (!formatted.isNullOrBlank()) formatted else (UserObject.getUserName(participantUser) ?: "")
        } else ""

        val isCreator = if (currentUserId != 0L) {
            chat.admin_id == currentUserId
        } else {
            chat.admin_id != 0L && chat.admin_id != userId
        }

        return SecretChatModel(
            chatId = chat.id,
            dialogId = dialogId,
            userId = userId,
            userName = userName,
            state = mapState(chat),
            ttlSeconds = chat.ttl,
            keyFingerprint = chat.key_fingerprint,
            isCreator = isCreator,
            createdAt = chat.date
        )
    }
}
