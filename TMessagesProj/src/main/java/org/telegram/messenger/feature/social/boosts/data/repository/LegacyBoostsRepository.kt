package org.telegram.messenger.feature.social.boosts.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.ChannelBoostsController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.boosts.data.mapper.BoostMapper
import org.telegram.messenger.feature.social.boosts.domain.model.BoostStatusModel
import org.telegram.messenger.feature.social.boosts.domain.model.CanApplyBoostModel
import org.telegram.messenger.feature.social.boosts.domain.model.MyBoostsModel
import org.telegram.messenger.feature.social.boosts.domain.repository.BoostsRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_stories
import kotlin.coroutines.resume

class LegacyBoostsRepository(
    private val currentAccount: Int,
    private val channelBoostsControllerProvider: () -> ChannelBoostsController = {
        MessagesController.getInstance(currentAccount).boostsController
    },
    private val messagesControllerProvider: () -> MessagesController = {
        MessagesController.getInstance(currentAccount)
    },
    private val connectionsManagerProvider: () -> ConnectionsManager = {
        ConnectionsManager.getInstance(currentAccount)
    }
) : BoostsRepository {

    override suspend fun getBoostsStatus(dialogId: Long): Result<BoostStatusModel> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine<Result<BoostStatusModel>> { continuation ->
            val normalizedId = if (dialogId < 0) dialogId else -dialogId
            val req = TL_stories.TL_premium_getBoostsStatus().apply {
                peer = messagesControllerProvider().getInputPeer(normalizedId)
            }
            val reqId = connectionsManagerProvider().sendRequest(req, { response, error ->
                if (response is TL_stories.TL_premium_boostsStatus) {
                    continuation.resume(Result.Success(BoostMapper.mapToBoostStatus(response)))
                } else {
                    continuation.resume(Result.Failure(mapTlError(error, "Failed to get boosts status")))
                }
            })
            continuation.invokeOnCancellation {
                connectionsManagerProvider().cancelRequest(reqId, true)
            }
        }
    }

    override suspend fun getMyBoosts(): Result<MyBoostsModel> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine<Result<MyBoostsModel>> { continuation ->
            val req = TL_stories.TL_premium_getMyBoosts()
            val reqId = connectionsManagerProvider().sendRequest(req, { response, error ->
                if (response is TL_stories.TL_premium_myBoosts) {
                    messagesControllerProvider().putUsers(response.users, false)
                    messagesControllerProvider().putChats(response.chats, false)
                    continuation.resume(Result.Success(BoostMapper.mapToMyBoosts(response)))
                } else {
                    continuation.resume(Result.Failure(mapTlError(error, "Failed to get my boosts")))
                }
            })
            continuation.invokeOnCancellation {
                connectionsManagerProvider().cancelRequest(reqId, true)
            }
        }
    }

    override suspend fun checkCanApplyBoost(dialogId: Long): Result<CanApplyBoostModel> = withContext(Dispatchers.IO) {
        val normalizedId = if (dialogId < 0) dialogId else -dialogId
        val statusReq = TL_stories.TL_premium_getBoostsStatus().apply {
            peer = messagesControllerProvider().getInputPeer(normalizedId)
        }

        val statusResult = suspendCancellableCoroutine<Result<TL_stories.TL_premium_boostsStatus>> { cont ->
            val reqId = connectionsManagerProvider().sendRequest(statusReq, { response, error ->
                if (response is TL_stories.TL_premium_boostsStatus) {
                    cont.resume(Result.Success(response))
                } else {
                    cont.resume(Result.Failure(mapTlError(error, "Failed to check boosts status")))
                }
            })
            cont.invokeOnCancellation {
                connectionsManagerProvider().cancelRequest(reqId, true)
            }
        }

        val status = when (statusResult) {
            is Result.Success -> statusResult.data
            is Result.Failure -> return@withContext Result.Failure(statusResult.error)
        }

        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<Result<CanApplyBoostModel>> { cont ->
                channelBoostsControllerProvider().userCanBoostChannel(normalizedId, status) { canApply ->
                    if (canApply != null) {
                        cont.resume(Result.Success(BoostMapper.mapToCanApplyBoost(canApply)))
                    } else {
                        cont.resume(Result.Failure(AppError.Generic("Failed to determine boost eligibility")))
                    }
                }
            }
        }
    }

    override suspend fun applyBoost(dialogId: Long, slots: List<Int>): Result<MyBoostsModel> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine<Result<MyBoostsModel>> { continuation ->
            val normalizedId = if (dialogId < 0) dialogId else -dialogId
            val req = TL_stories.TL_premium_applyBoost().apply {
                peer = messagesControllerProvider().getInputPeer(normalizedId)
                flags = flags or 1
                this.slots.addAll(slots)
            }
            val reqId = connectionsManagerProvider().sendRequest(
                req,
                { response, error ->
                    if (response is TL_stories.TL_premium_myBoosts) {
                        messagesControllerProvider().putUsers(response.users, false)
                        messagesControllerProvider().putChats(response.chats, false)
                        continuation.resume(Result.Success(BoostMapper.mapToMyBoosts(response)))
                    } else {
                        continuation.resume(Result.Failure(mapTlError(error, "Failed to apply boost")))
                    }
                },
                ConnectionsManager.RequestFlagInvokeAfter or ConnectionsManager.RequestFlagFailOnServerErrors
            )
            continuation.invokeOnCancellation {
                connectionsManagerProvider().cancelRequest(reqId, true)
            }
        }
    }

    private fun mapTlError(error: TLRPC.TL_error?, defaultMessage: String): AppError {
        return if (error != null) {
            AppError.Network("$defaultMessage: ${error.text} (${error.code})", code = error.code)
        } else {
            AppError.Generic(defaultMessage)
        }
    }
}
