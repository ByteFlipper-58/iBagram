package org.telegram.messenger.feature.messaging.messagecustomparams.data.datasource

import org.telegram.messenger.MessageCustomParamsHelper
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.model.MessageCustomParamsModel
import org.telegram.tgnet.NativeByteBuffer
import org.telegram.tgnet.TLRPC
import java.util.concurrent.ConcurrentHashMap

/**
 * Local data source managing message custom params in memory and SQLite serialized buffers.
 */
open class MessageCustomParamsLocalDataSource(
    private val account: Int = 0
) {

    private val paramsCache = ConcurrentHashMap<Long, MessageCustomParamsModel>()

    open fun getParams(messageId: Long): MessageCustomParamsModel? {
        return paramsCache[messageId]
    }

    open fun setParams(messageId: Long, params: MessageCustomParamsModel) {
        paramsCache[messageId] = params
    }

    open fun copyParams(fromMessageId: Long, toMessageId: Long) {
        val source = paramsCache[fromMessageId]
        if (source != null) {
            paramsCache[toMessageId] = source.copy(messageId = toMessageId)
        }
    }

    open fun removeParams(messageId: Long) {
        paramsCache.remove(messageId)
    }

    open fun clearAll() {
        paramsCache.clear()
    }

    open fun getCacheSize(): Int {
        return paramsCache.size
    }

    open fun readLocalParams(message: TLRPC.Message, byteBuffer: NativeByteBuffer?) {
        if (byteBuffer == null) return
        try {
            MessageCustomParamsHelper.readLocalParams(message, byteBuffer)
        } catch (_: Throwable) {
            // Safe fallback for tests or malformed buffers
        }
    }

    open fun writeLocalParams(message: TLRPC.Message): NativeByteBuffer? {
        return try {
            MessageCustomParamsHelper.writeLocalParams(message)
        } catch (_: Throwable) {
            null
        }
    }

    open fun isMessageEmpty(message: TLRPC.Message): Boolean {
        return try {
            MessageCustomParamsHelper.isEmpty(message)
        } catch (_: Throwable) {
            true
        }
    }
}
