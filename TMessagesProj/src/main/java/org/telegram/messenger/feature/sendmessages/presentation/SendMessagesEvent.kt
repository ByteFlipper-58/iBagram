package org.telegram.messenger.feature.sendmessages.presentation

import org.telegram.messenger.feature.sendmessages.domain.model.ForwardRequestModel
import org.telegram.messenger.feature.sendmessages.domain.model.SendMediaItem
import org.telegram.messenger.feature.sendmessages.domain.model.SendOptionsModel

sealed class SendMessagesEvent {
    data class SendText(
        val dialogId: Long,
        val text: String,
        val options: SendOptionsModel = SendOptionsModel(),
        val isPremium: Boolean = false
    ) : SendMessagesEvent()

    data class SendMedia(
        val dialogId: Long,
        val item: SendMediaItem,
        val options: SendOptionsModel = SendOptionsModel(),
        val isPremium: Boolean = false
    ) : SendMessagesEvent()

    data class SendAlbum(
        val dialogId: Long,
        val items: List<SendMediaItem>,
        val options: SendOptionsModel = SendOptionsModel()
    ) : SendMessagesEvent()

    data class Forward(
        val request: ForwardRequestModel
    ) : SendMessagesEvent()

    data class Retry(
        val localId: Long
    ) : SendMessagesEvent()

    data class Cancel(
        val localId: Long
    ) : SendMessagesEvent()

    data class CancelAll(
        val dialogId: Long? = null
    ) : SendMessagesEvent()
}
