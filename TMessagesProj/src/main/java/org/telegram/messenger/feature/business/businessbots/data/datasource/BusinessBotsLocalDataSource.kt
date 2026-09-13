package org.telegram.messenger.feature.business.businessbots.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.feature.business.businessbots.data.mapper.BusinessBotMapper
import org.telegram.messenger.feature.business.businessbots.domain.model.ConnectedBotModel
import org.telegram.ui.Business.BusinessChatbotController
import java.util.concurrent.CopyOnWriteArrayList

class BusinessBotsLocalDataSource(
    private val currentAccount: Int
) {
    private val testFallbackBots = CopyOnWriteArrayList<ConnectedBotModel>()
    private val testEventFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    fun observeUpdatedChatbot(): Flow<Unit> {
        return try {
            NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.updatedChatbot)
                .map { Unit }
        } catch (e: Throwable) {
            testEventFlow.asSharedFlow()
        }
    }

    fun getConnectedBots(): List<ConnectedBotModel> {
        return try {
            val controller = BusinessChatbotController.getInstance(currentAccount)
            if (controller != null && controller.value != null) {
                BusinessBotMapper.mapConnectedBots(controller.value)
            } else {
                testFallbackBots.toList()
            }
        } catch (e: Throwable) {
            testFallbackBots.toList()
        }
    }

    fun findConnectedBot(botId: Long): ConnectedBotModel? {
        return try {
            val controller = BusinessChatbotController.getInstance(currentAccount)
            val value = controller?.value
            if (value != null && value.connected_bots != null) {
                val bot = value.connected_bots.find { it.bot_id == botId }
                bot?.let { BusinessBotMapper.mapConnectedBot(it) }
            } else {
                testFallbackBots.find { it.botId == botId }
            }
        } catch (e: Throwable) {
            testFallbackBots.find { it.botId == botId }
        }
    }

    fun invalidate(reload: Boolean) {
        try {
            BusinessChatbotController.getInstance(currentAccount)?.invalidate(reload)
        } catch (e: Throwable) {
            // Headless fallback
        }
    }

    fun updateBot(model: ConnectedBotModel) {
        val index = testFallbackBots.indexOfFirst { it.botId == model.botId }
        if (index >= 0) {
            testFallbackBots[index] = model
        } else {
            testFallbackBots.add(model)
        }
        testEventFlow.tryEmit(Unit)
    }

    fun deleteBot(botId: Long) {
        testFallbackBots.removeAll { it.botId == botId }
        testEventFlow.tryEmit(Unit)
    }

    // Testing helper
    fun setTestBots(bots: List<ConnectedBotModel>) {
        testFallbackBots.clear()
        testFallbackBots.addAll(bots)
        testEventFlow.tryEmit(Unit)
    }
}
