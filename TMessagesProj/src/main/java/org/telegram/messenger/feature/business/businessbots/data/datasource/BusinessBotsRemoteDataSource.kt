package org.telegram.messenger.feature.business.businessbots.data.datasource

import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.businessbots.data.mapper.BusinessBotMapper
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRecipientsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRightsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.ConnectedBotModel
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

class BusinessBotsRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    suspend fun getConnectedBots(): Result<List<ConnectedBotModel>> {
        val req = TL_account.getConnectedBots()
        return when (val res = executeRequest<TLObject>(req)) {
            is Result.Success -> {
                val data = res.data
                if (data is TL_account.connectedBots) {
                    Result.Success(BusinessBotMapper.mapConnectedBots(data))
                } else {
                    Result.Success(emptyList())
                }
            }
            is Result.Failure -> Result.Failure(res.error)
        }
    }

    suspend fun updateConnectedBot(
        botId: Long,
        rights: BusinessBotRightsModel,
        recipients: BusinessBotRecipientsModel
    ): Result<ConnectedBotModel> {
        val req = TL_account.updateConnectedBot()
        req.deleted = false
        req.rights = BusinessBotMapper.toTlRights(rights)
        val user = try {
            MessagesController.getInstance(currentAccount)?.getUser(botId)
        } catch (e: Throwable) {
            null
        }
        req.bot = try {
            MessagesController.getInstance(currentAccount)?.getInputUser(user) ?: run {
                val inputUser = TLRPC.TL_inputUser()
                inputUser.user_id = botId
                inputUser
            }
        } catch (e: Throwable) {
            val inputUser = TLRPC.TL_inputUser()
            inputUser.user_id = botId
            inputUser
        }
        req.recipients = try {
            BusinessBotMapper.toTlInputRecipients(recipients, currentAccount)
        } catch (e: Throwable) {
            TL_account.TL_inputBusinessBotRecipients()
        }

        return when (val res = executeRequest<TLObject>(req)) {
            is Result.Success -> {
                val updatedModel = ConnectedBotModel(
                    botId = botId,
                    recipients = recipients,
                    rights = rights
                )
                Result.Success(updatedModel)
            }
            is Result.Failure -> Result.Failure(res.error)
        }
    }

    suspend fun deleteConnectedBot(botId: Long): Result<Unit> {
        val req = TL_account.updateConnectedBot()
        req.deleted = true
        val user = try {
            MessagesController.getInstance(currentAccount)?.getUser(botId)
        } catch (e: Throwable) {
            null
        }
        req.bot = try {
            MessagesController.getInstance(currentAccount)?.getInputUser(user) ?: run {
                val inputUser = TLRPC.TL_inputUser()
                inputUser.user_id = botId
                inputUser
            }
        } catch (e: Throwable) {
            val inputUser = TLRPC.TL_inputUser()
            inputUser.user_id = botId
            inputUser
        }
        req.recipients = TL_account.TL_inputBusinessBotRecipients()

        return when (val res = executeRequest<TLObject>(req)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(res.error)
        }
    }
}
