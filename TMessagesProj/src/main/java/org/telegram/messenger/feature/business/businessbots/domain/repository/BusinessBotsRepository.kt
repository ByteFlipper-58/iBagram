package org.telegram.messenger.feature.business.businessbots.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRecipientsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRightsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.ConnectedBotModel

interface BusinessBotsRepository {
    fun observeConnectedBots(): Flow<List<ConnectedBotModel>>
    suspend fun getConnectedBots(): List<ConnectedBotModel>
    suspend fun loadConnectedBots(forceReload: Boolean = false): Result<List<ConnectedBotModel>>
    suspend fun updateConnectedBot(
        botId: Long,
        rights: BusinessBotRightsModel,
        recipients: BusinessBotRecipientsModel
    ): Result<ConnectedBotModel>
    suspend fun deleteConnectedBot(botId: Long): Result<Unit>
    fun findConnectedBot(botId: Long): ConnectedBotModel?
}
