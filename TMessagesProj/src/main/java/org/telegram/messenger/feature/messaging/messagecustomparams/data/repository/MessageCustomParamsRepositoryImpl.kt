package org.telegram.messenger.feature.messaging.messagecustomparams.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.messaging.messagecustomparams.data.datasource.MessageCustomParamsLocalDataSource
import org.telegram.messenger.feature.messaging.messagecustomparams.data.datasource.MessageCustomParamsRemoteDataSource
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.model.MessageCustomParamsModel
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.model.MessageCustomParamsState
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.repository.MessageCustomParamsRepository

/**
 * Clean repository implementation managing message custom params and reactive state.
 */
class MessageCustomParamsRepositoryImpl(
    private val currentAccount: Int = 0,
    private val remoteDataSource: MessageCustomParamsRemoteDataSource = MessageCustomParamsRemoteDataSource(currentAccount),
    private val localDataSource: MessageCustomParamsLocalDataSource = MessageCustomParamsLocalDataSource(currentAccount),
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : MessageCustomParamsRepository {

    private val _state = MutableStateFlow(MessageCustomParamsState())

    override fun observeState(): Flow<MessageCustomParamsState> = _state.asStateFlow()

    override fun getState(): MessageCustomParamsState = _state.value

    override fun getParamsForMessage(messageId: Long): MessageCustomParamsModel? {
        return localDataSource.getParams(messageId)
    }

    override fun setParamsForMessage(messageId: Long, params: MessageCustomParamsModel) {
        localDataSource.setParams(messageId, params)
        _state.update {
            it.copy(
                cachedParamsCount = localDataSource.getCacheSize(),
                lastUpdatedMessageId = messageId
            )
        }
    }

    override fun copyParams(fromMessageId: Long, toMessageId: Long) {
        localDataSource.copyParams(fromMessageId, toMessageId)
        _state.update {
            it.copy(
                cachedParamsCount = localDataSource.getCacheSize(),
                lastUpdatedMessageId = toMessageId
            )
        }
    }

    override fun removeParams(messageId: Long) {
        localDataSource.removeParams(messageId)
        _state.update {
            it.copy(
                cachedParamsCount = localDataSource.getCacheSize(),
                lastUpdatedMessageId = messageId
            )
        }
    }

    override fun clearAll() {
        localDataSource.clearAll()
        _state.update {
            it.copy(
                cachedParamsCount = 0,
                lastUpdatedMessageId = 0L
            )
        }
    }
}
