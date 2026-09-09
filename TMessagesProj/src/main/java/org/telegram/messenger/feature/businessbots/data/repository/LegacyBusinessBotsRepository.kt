package org.telegram.messenger.feature.businessbots.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.Utilities
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.businessbots.data.mapper.BusinessBotMapper
import org.telegram.messenger.feature.businessbots.domain.model.BusinessBotRecipientsModel
import org.telegram.messenger.feature.businessbots.domain.model.BusinessBotRightsModel
import org.telegram.messenger.feature.businessbots.domain.model.ConnectedBotModel
import org.telegram.messenger.feature.businessbots.domain.repository.BusinessBotsRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import org.telegram.ui.Business.BusinessChatbotController
import kotlin.coroutines.resume

class LegacyBusinessBotsRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : BusinessBotsRepository {

    override fun observeConnectedBots(): Flow<List<ConnectedBotModel>> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.updatedChatbot)
            .map { getConnectedBots() }
            .onStart { emit(getConnectedBots()) }
            .flowOn(mainDispatcher)
    }

    override suspend fun getConnectedBots(): List<ConnectedBotModel> = withContext(mainDispatcher) {
        val controller = BusinessChatbotController.getInstance(currentAccount)
        BusinessBotMapper.mapConnectedBots(controller.value)
    }

    override suspend fun loadConnectedBots(forceReload: Boolean): Result<List<ConnectedBotModel>> = withContext(mainDispatcher) {
        val controller = BusinessChatbotController.getInstance(currentAccount)
        if (forceReload) {
            controller.invalidate(false)
        }

        suspendCancellableCoroutine { cont ->
            controller.load { bots ->
                val result = BusinessBotMapper.mapConnectedBots(bots)
                cont.resume(Result.Success(result))
            }
        }
    }

    override suspend fun updateConnectedBot(
        botId: Long,
        rights: BusinessBotRightsModel,
        recipients: BusinessBotRecipientsModel
    ): Result<ConnectedBotModel> = withContext(mainDispatcher) {
        suspendCancellableCoroutine { cont ->
            val req = TL_account.updateConnectedBot()
            req.deleted = false
            req.rights = BusinessBotMapper.toTlRights(rights)
            val user = MessagesController.getInstance(currentAccount).getUser(botId)
            req.bot = MessagesController.getInstance(currentAccount).getInputUser(user) ?: run {
                val inputUser = TLRPC.TL_inputUser()
                inputUser.user_id = botId
                inputUser
            }
            req.recipients = BusinessBotMapper.toTlInputRecipients(recipients, currentAccount)

            val reqId = ConnectionsManager.getInstance(currentAccount).sendRequest(req) { res, err ->
                AndroidUtilities.runOnUIThread {
                    if (err != null) {
                        cont.resume(Result.failure(err.text ?: "Failed to update business bot"))
                    } else if (res is TLRPC.TL_boolFalse) {
                        cont.resume(Result.failure("Unknown error updating business bot"))
                    } else {
                        if (res is TLRPC.Updates) {
                            Utilities.stageQueue.postRunnable {
                                MessagesController.getInstance(currentAccount).processUpdates(res, false)
                            }
                        }
                        BusinessChatbotController.getInstance(currentAccount).invalidate(true)

                        val updatedModel = ConnectedBotModel(
                            botId = botId,
                            recipients = recipients,
                            rights = rights
                        )
                        cont.resume(Result.Success(updatedModel))
                    }
                }
            }

            cont.invokeOnCancellation {
                ConnectionsManager.getInstance(currentAccount).cancelRequest(reqId, true)
            }
        }
    }

    override suspend fun deleteConnectedBot(botId: Long): Result<Unit> = withContext(mainDispatcher) {
        suspendCancellableCoroutine { cont ->
            val req = TL_account.updateConnectedBot()
            req.deleted = true
            val user = MessagesController.getInstance(currentAccount).getUser(botId)
            req.bot = MessagesController.getInstance(currentAccount).getInputUser(user) ?: run {
                val inputUser = TLRPC.TL_inputUser()
                inputUser.user_id = botId
                inputUser
            }
            req.recipients = TL_account.TL_inputBusinessBotRecipients()

            val reqId = ConnectionsManager.getInstance(currentAccount).sendRequest(req) { res, err ->
                AndroidUtilities.runOnUIThread {
                    if (err != null) {
                        cont.resume(Result.failure(err.text ?: "Failed to delete business bot"))
                    } else if (res is TLRPC.TL_boolFalse) {
                        cont.resume(Result.failure("Unknown error deleting business bot"))
                    } else {
                        if (res is TLRPC.Updates) {
                            Utilities.stageQueue.postRunnable {
                                MessagesController.getInstance(currentAccount).processUpdates(res, false)
                            }
                        }
                        BusinessChatbotController.getInstance(currentAccount).invalidate(true)
                        cont.resume(Result.Success(Unit))
                    }
                }
            }

            cont.invokeOnCancellation {
                ConnectionsManager.getInstance(currentAccount).cancelRequest(reqId, true)
            }
        }
    }

    override fun findConnectedBot(botId: Long): ConnectedBotModel? {
        val controller = BusinessChatbotController.getInstance(currentAccount)
        val value = controller.value ?: return null
        val bot = value.connected_bots?.find { it.bot_id == botId } ?: return null
        return BusinessBotMapper.mapConnectedBot(bot)
    }
}
