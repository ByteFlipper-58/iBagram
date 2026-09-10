package org.telegram.messenger.feature.messaging.autodelete.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.autodelete.domain.model.AutoDeleteTtlModel
import org.telegram.messenger.feature.messaging.autodelete.domain.model.GlobalAutoDeleteStateModel

interface AutoDeleteRepository {
    fun observeGlobalAutoDelete(): Flow<GlobalAutoDeleteStateModel>
    suspend fun getGlobalAutoDelete(forceRefresh: Boolean = false): Result<AutoDeleteTtlModel>
    suspend fun setGlobalAutoDelete(ttl: AutoDeleteTtlModel): Result<Unit>
    suspend fun getChatAutoDelete(chatId: Long): Result<AutoDeleteTtlModel>
    suspend fun setChatAutoDelete(chatId: Long, ttl: AutoDeleteTtlModel): Result<Unit>
    suspend fun setChatsAutoDeleteBatch(chatIds: List<Long>, ttl: AutoDeleteTtlModel): Result<Unit>
}
