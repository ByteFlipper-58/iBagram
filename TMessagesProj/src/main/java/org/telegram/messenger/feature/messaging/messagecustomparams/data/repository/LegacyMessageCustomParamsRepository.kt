package org.telegram.messenger.feature.messaging.messagecustomparams.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.model.MessageCustomParamsModel
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.model.MessageCustomParamsState
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.repository.MessageCustomParamsRepository
import java.util.concurrent.ConcurrentHashMap

class LegacyMessageCustomParamsRepository(
    private val currentAccount: Int = 0
) : MessageCustomParamsRepository {

    private val paramsCache = ConcurrentHashMap<Long, MessageCustomParamsModel>()
    private val _state = MutableStateFlow(MessageCustomParamsState())

    override fun observeState(): Flow<MessageCustomParamsState> = _state.asStateFlow()

    override fun getState(): MessageCustomParamsState = _state.value

    override fun getParamsForMessage(messageId: Long): MessageCustomParamsModel? {
        return paramsCache[messageId]
    }

    override fun setParamsForMessage(messageId: Long, params: MessageCustomParamsModel) {
        paramsCache[messageId] = params
        _state.update {
            it.copy(
                cachedParamsCount = paramsCache.size,
                lastUpdatedMessageId = messageId
            )
        }
    }

    override fun copyParams(fromMessageId: Long, toMessageId: Long) {
        val source = paramsCache[fromMessageId]
        if (source != null) {
            paramsCache[toMessageId] = source.copy(messageId = toMessageId)
            _state.update {
                it.copy(
                    cachedParamsCount = paramsCache.size,
                    lastUpdatedMessageId = toMessageId
                )
            }
        }
    }

    override fun removeParams(messageId: Long) {
        paramsCache.remove(messageId)
        _state.update {
            it.copy(
                cachedParamsCount = paramsCache.size,
                lastUpdatedMessageId = messageId
            )
        }
    }

    override fun clearAll() {
        paramsCache.clear()
        _state.update {
            it.copy(
                cachedParamsCount = 0,
                lastUpdatedMessageId = 0L
            )
        }
    }
}
