package org.telegram.messenger.feature.emojieffects.data.mapper

import org.telegram.messenger.feature.emojieffects.domain.model.EmojiInteractionAction
import org.telegram.messenger.feature.emojieffects.domain.model.EmojiInteractionSession
import org.telegram.messenger.feature.emojieffects.domain.usecase.DecodeEmojiInteractionsJsonUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.EncodeEmojiInteractionsJsonUseCase
import org.telegram.tgnet.TLRPC

/**
 * Маппер между доменными моделями интерактивных эмодзи и TL/MTProto структурами Telegram.
 */
class EmojiEffectsMapper(
    private val encodeEmojiInteractionsJsonUseCase: EncodeEmojiInteractionsJsonUseCase = EncodeEmojiInteractionsJsonUseCase(),
    private val decodeEmojiInteractionsJsonUseCase: DecodeEmojiInteractionsJsonUseCase = DecodeEmojiInteractionsJsonUseCase()
) {

    fun toTlInteraction(session: EmojiInteractionSession): TLRPC.TL_sendMessageEmojiInteraction {
        val tl = TLRPC.TL_sendMessageEmojiInteraction()
        tl.msg_id = session.messageId
        tl.emoticon = session.emoticon
        tl.interaction = TLRPC.TL_dataJSON().apply {
            data = encodeEmojiInteractionsJsonUseCase(session)
        }
        return tl
    }

    fun toActions(tlInteraction: TLRPC.TL_sendMessageEmojiInteraction?): List<EmojiInteractionAction> {
        val json = tlInteraction?.interaction?.data ?: return emptyList()
        return decodeEmojiInteractionsJsonUseCase(json)
    }
}
