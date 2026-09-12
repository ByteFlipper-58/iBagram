package org.telegram.messenger.feature.messaging.translate.data.datasource

import org.telegram.messenger.TranslateController
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC

/**
 * Remote data source for executing MTProto RPCs related to text translation.
 */
open class TranslationRemoteDataSource(currentAccount: Int) : BaseRemoteDataSource(currentAccount) {

    open suspend fun translateText(text: String, toLanguage: String): Result<TLRPC.TL_messages_translateResult> {
        val req = TLRPC.TL_messages_translateText().apply {
            this.flags = 2 // raw text mode
            val textEntity = TLRPC.TL_textWithEntities().apply {
                this.text = text
            }
            this.text.add(textEntity)
            this.to_lang = try {
                TranslateController.normalizeLanguage(toLanguage)
            } catch (_: Throwable) {
                toLanguage
            }
        }
        return executeRequest(req)
    }
}
