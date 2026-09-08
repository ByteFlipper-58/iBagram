package org.telegram.messenger.feature.factcheck.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.factcheck.data.mapper.FactCheckMapper
import org.telegram.messenger.feature.factcheck.domain.model.FactCheckEntityModel
import org.telegram.messenger.feature.factcheck.domain.model.FactCheckModel
import org.telegram.messenger.feature.factcheck.domain.repository.FactCheckRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.Vector
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

class LegacyFactCheckRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : FactCheckRepository {

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(currentAccount)

    private val connectionsManager: ConnectionsManager
        get() = ConnectionsManager.getInstance(currentAccount)

    private val notificationCenter: NotificationCenter
        get() = NotificationCenter.getInstance(currentAccount)

    private val localCache = ConcurrentHashMap<String, FactCheckModel>()

    override fun observeFactCheckLoaded(): Flow<Unit> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.factCheckLoaded)
            .map { }
            .flowOn(mainDispatcher)
    }

    override suspend fun getFactCheck(dialogId: Long, messageId: Int, hash: Long): FactCheckModel? {
        val key = "$dialogId:$messageId"
        localCache[key]?.let { return it }
        if (hash != 0L) {
            localCache["hash:$hash"]?.let { return it }
        }
        return null
    }

    override suspend fun loadFactCheck(dialogId: Long, messageId: Int): Result<FactCheckModel?> =
        withContext(mainDispatcher) {
            val peer = messagesController.getInputPeer(dialogId)
                ?: return@withContext Result.Failure(AppError.NotFound("InputPeer not found for dialogId $dialogId"))

            val req = TLRPC.TL_getFactCheck().apply {
                this.peer = peer
                this.msg_id.add(messageId)
            }

            suspendCancellableCoroutine<Result<FactCheckModel?>> { continuation ->
                val requestId = connectionsManager.sendRequest(req) { response, error ->
                    if (continuation.isActive) {
                        if (error == null) {
                            var factCheck: FactCheckModel? = null
                            if (response is Vector<*>) {
                                for (obj in response.objects) {
                                    if (obj is TLRPC.TL_factCheck) {
                                        factCheck = FactCheckMapper.toDomain(obj, dialogId, messageId)
                                        break
                                    }
                                }
                            } else if (response is TLRPC.TL_factCheck) {
                                factCheck = FactCheckMapper.toDomain(response, dialogId, messageId)
                            }
                            if (factCheck != null) {
                                localCache["$dialogId:$messageId"] = factCheck
                                localCache["hash:${factCheck.hash}"] = factCheck
                            }
                            continuation.resume(Result.Success(factCheck))
                        } else {
                            continuation.resume(
                                Result.Failure(AppError.Network(error.text ?: "Failed to load fact check", error.code))
                            )
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    connectionsManager.cancelRequest(requestId, false)
                }
            }
        }

    override suspend fun applyFactCheck(
        dialogId: Long,
        messageId: Int,
        text: String,
        entities: List<FactCheckEntityModel>?
    ): Result<Unit> = withContext(mainDispatcher) {
        val peer = messagesController.getInputPeer(dialogId)
            ?: return@withContext Result.Failure(AppError.NotFound("InputPeer not found for dialogId $dialogId"))

        val req = TLRPC.TL_editFactCheck().apply {
            this.peer = peer
            this.msg_id = messageId
            this.text = FactCheckMapper.toTlTextWithEntities(text, entities)
        }

        suspendCancellableCoroutine<Result<Unit>> { continuation ->
            val requestId = connectionsManager.sendRequest(req) { response, error ->
                if (continuation.isActive) {
                    if (error == null) {
                        if (response is TLRPC.Updates) {
                            messagesController.processUpdates(response, false)
                        }
                        val updatedModel = FactCheckModel(
                            hash = 0L,
                            dialogId = dialogId,
                            messageId = messageId,
                            text = text,
                            entities = entities ?: emptyList(),
                            country = null,
                            needCheck = false
                        )
                        localCache["$dialogId:$messageId"] = updatedModel
                        notificationCenter.postNotificationName(NotificationCenter.factCheckLoaded)
                        continuation.resume(Result.Success(Unit))
                    } else {
                        continuation.resume(
                            Result.Failure(AppError.Network(error.text ?: "Failed to edit fact check", error.code))
                        )
                    }
                }
            }

            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(requestId, false)
            }
        }
    }

    override suspend fun deleteFactCheck(dialogId: Long, messageId: Int): Result<Unit> =
        withContext(mainDispatcher) {
            val peer = messagesController.getInputPeer(dialogId)
                ?: return@withContext Result.Failure(AppError.NotFound("InputPeer not found for dialogId $dialogId"))

            val req = TLRPC.TL_deleteFactCheck().apply {
                this.peer = peer
                this.msg_id = messageId
            }

            suspendCancellableCoroutine<Result<Unit>> { continuation ->
                val requestId = connectionsManager.sendRequest(req) { response, error ->
                    if (continuation.isActive) {
                        if (error == null) {
                            if (response is TLRPC.Updates) {
                                messagesController.processUpdates(response, false)
                            }
                            localCache.remove("$dialogId:$messageId")
                            notificationCenter.postNotificationName(NotificationCenter.factCheckLoaded)
                            continuation.resume(Result.Success(Unit))
                        } else {
                            continuation.resume(
                                Result.Failure(AppError.Network(error.text ?: "Failed to delete fact check", error.code))
                            )
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    connectionsManager.cancelRequest(requestId, false)
                }
            }
        }

    override suspend fun getFactCheckLimit(): Int = withContext(mainDispatcher) {
        val limit = messagesController.factcheckLengthLimit
        if (limit > 0) limit else 1024
    }
}
