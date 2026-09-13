package org.telegram.messenger.feature.messaging.chatmeta.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.telegram.tgnet.ConnectionsManager
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chatmeta.data.datasource.ChatMetadataLocalDataSource
import org.telegram.messenger.feature.messaging.chatmeta.data.datasource.ChatMetadataRemoteDataSource
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.ChatMetadataBatchResult
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.ChatMetadataStatsModel
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.MessageMetadataCheckItem
import org.telegram.messenger.feature.messaging.chatmeta.domain.repository.ChatMessagesMetadataRepository
import org.telegram.tgnet.TLRPC

/**
 * Clean repository implementation coordinating [ChatMetadataLocalDataSource] and [ChatMetadataRemoteDataSource].
 */
open class ChatMetadataRepositoryImpl(
    private val account: Int,
    private val localDataSource: ChatMetadataLocalDataSource,
    private val remoteDataSource: ChatMetadataRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ChatMessagesMetadataRepository {

    private val scope = CoroutineScope(mainDispatcher)
    private val _stats = MutableStateFlow(localDataSource.getStats())

    override fun observeStats(): Flow<ChatMetadataStatsModel> = _stats.asStateFlow()

    override fun getStats(): ChatMetadataStatsModel = localDataSource.getStats()

    override fun checkMessages(
        dialogId: Long,
        items: List<MessageMetadataCheckItem>,
        currentTime: Long
    ): Result<ChatMetadataBatchResult> {
        val batchResult = localDataSource.inspectItemsForUpdates(dialogId, items, currentTime)
        _stats.value = localDataSource.getStats()

        if (batchResult.reactionMessageIds.isNotEmpty()) {
            loadReactions(dialogId, batchResult.reactionMessageIds)
        }
        if (batchResult.extendedMediaMessageIds.isNotEmpty()) {
            loadExtendedMedia(dialogId, batchResult.extendedMediaMessageIds)
        }

        return Result.Success(batchResult)
    }

    override fun loadReactions(dialogId: Long, messageIds: List<Int>): Result<Unit> {
        if (messageIds.isEmpty()) return Result.Success(Unit)

        val peer = localDataSource.getInputPeer(dialogId)
        if (peer == null) {
            return Result.Success(Unit)
        }

        val req = TLRPC.TL_messages_getMessagesReactions()
        req.peer = peer
        for (id in messageIds) {
            req.id.add(id)
        }

        val cm = runCatching { ConnectionsManager.getInstance(account) }.getOrNull()
        if (cm != null) {
            val reqId = cm.sendRequest(req) { response, error ->
                localDataSource.completeReactionsRequest(0)
                if (error == null && response is TLRPC.Updates) {
                    localDataSource.processReactionsUpdates(response)
                }
                _stats.value = localDataSource.getStats()
            }
            localDataSource.trackReactionsRequest(reqId)
            _stats.value = localDataSource.getStats()
        } else {
            // Headless coroutine fallback
            scope.launch {
                val res = remoteDataSource.loadReactions(peer, messageIds)
                if (res is Result.Success) {
                    localDataSource.processReactionsUpdates(res.data)
                }
                _stats.value = localDataSource.getStats()
            }
        }

        return Result.Success(Unit)
    }

    override fun loadExtendedMedia(dialogId: Long, messageIds: List<Int>): Result<Unit> {
        if (messageIds.isEmpty()) return Result.Success(Unit)

        val peer = localDataSource.getInputPeer(dialogId)
        if (peer == null) {
            return Result.Success(Unit)
        }

        val req = TLRPC.TL_messages_getExtendedMedia()
        req.peer = peer
        for (id in messageIds) {
            req.id.add(id)
        }

        val cm = runCatching { ConnectionsManager.getInstance(account) }.getOrNull()
        if (cm != null) {
            val reqId = cm.sendRequest(req) { response, error ->
                localDataSource.completeExtendedMediaRequest(0)
                if (error == null && response is TLRPC.Updates) {
                    localDataSource.processExtendedMediaUpdates(response)
                }
                _stats.value = localDataSource.getStats()
            }
            localDataSource.trackExtendedMediaRequest(reqId)
            _stats.value = localDataSource.getStats()
        } else {
            // Headless coroutine fallback
            scope.launch {
                val res = remoteDataSource.loadExtendedMedia(peer, messageIds)
                if (res is Result.Success) {
                    localDataSource.processExtendedMediaUpdates(res.data)
                }
                _stats.value = localDataSource.getStats()
            }
        }

        return Result.Success(Unit)
    }

    override fun cancelPendingRequests(): Result<Unit> {
        localDataSource.cancelAllPendingRequests()
        _stats.value = localDataSource.getStats()
        return Result.Success(Unit)
    }
}
