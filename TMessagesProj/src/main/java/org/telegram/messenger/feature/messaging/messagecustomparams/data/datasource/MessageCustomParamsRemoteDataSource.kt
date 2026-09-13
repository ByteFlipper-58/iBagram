package org.telegram.messenger.feature.messaging.messagecustomparams.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC

/**
 * Remote data source encapsulating MTProto requests for voice transcription and translation custom params.
 */
open class MessageCustomParamsRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun transcribeAudio(peer: TLRPC.InputPeer, msgId: Int): Result<TLRPC.TL_messages_transcribedAudio> {
        val req = TLRPC.TL_messages_transcribeAudio()
        req.peer = peer
        req.msg_id = msgId
        return executeRequest<TLRPC.TL_messages_transcribedAudio>(req)
    }

    open suspend fun translateText(text: String, toLang: String): Result<TLRPC.TL_messages_translateResult> {
        val req = TLRPC.TL_messages_translateText()
        req.to_lang = toLang
        val textWithEntities = TLRPC.TL_textWithEntities()
        textWithEntities.text = text
        req.text.add(textWithEntities)
        return executeRequest<TLRPC.TL_messages_translateResult>(req)
    }
}
