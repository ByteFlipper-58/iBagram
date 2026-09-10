package org.telegram.messenger.feature.messagecustomparams.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messagecustomparams.domain.model.MessageCustomParamsModel
import org.telegram.messenger.feature.messagecustomparams.domain.model.MessageCustomParamsState

interface MessageCustomParamsRepository {
    fun observeState(): Flow<MessageCustomParamsState>
    fun getState(): MessageCustomParamsState

    fun getParamsForMessage(messageId: Long): MessageCustomParamsModel?
    fun setParamsForMessage(messageId: Long, params: MessageCustomParamsModel)
    fun copyParams(fromMessageId: Long, toMessageId: Long)
    fun removeParams(messageId: Long)
    fun clearAll()
}
