package org.telegram.messenger.feature.business.botstars.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_payments
import org.telegram.tgnet.tl.TL_stars
import org.telegram.ui.Stars.BotStarsController
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Local data source managing cached Bot Stars revenue stats, TON revenue stats,
 * transactions, connected referral bots, suggested bots, and admined dialogs.
 */
open class BotStarsLocalDataSource(
    private val currentAccount: Int
) {
    private var isTestMode = false

    private val testStarsRevenueStats = ConcurrentHashMap<Long, TLRPC.TL_payments_starsRevenueStats>()
    private val testTonRevenueStats = ConcurrentHashMap<Long, TLRPC.TL_payments_starsRevenueStats>()
    private val testTransactions = ConcurrentHashMap<String, List<TL_stars.StarsTransaction>>()
    private val testConnectedBots = ConcurrentHashMap<Long, List<TL_payments.connectedBotStarRef>>()
    private val testSuggestedBots = ConcurrentHashMap<Long, List<TL_payments.starRefProgram>>()
    private val testAdminedBots = CopyOnWriteArrayList<Long>()
    private val testAdminedChannels = CopyOnWriteArrayList<Long>()

    private val testBotStarsUpdatedFlow = MutableSharedFlow<Long?>(extraBufferCapacity = 16)
    private val testTransactionsLoadedFlow = MutableSharedFlow<Long?>(extraBufferCapacity = 16)
    private val testConnectedBotsUpdateFlow = MutableSharedFlow<Long?>(extraBufferCapacity = 16)

    fun setTestMode(testMode: Boolean) {
        this.isTestMode = testMode
    }

    fun setTestStarsRevenueStats(dialogId: Long, stats: TLRPC.TL_payments_starsRevenueStats) {
        testStarsRevenueStats[dialogId] = stats
        testBotStarsUpdatedFlow.tryEmit(dialogId)
    }

    fun setTestTonRevenueStats(dialogId: Long, stats: TLRPC.TL_payments_starsRevenueStats) {
        testTonRevenueStats[dialogId] = stats
        testBotStarsUpdatedFlow.tryEmit(dialogId)
    }

    fun setTestTransactions(dialogId: Long, type: Int, txs: List<TL_stars.StarsTransaction>) {
        testTransactions["${dialogId}_$type"] = txs
        testTransactionsLoadedFlow.tryEmit(dialogId)
    }

    fun setTestConnectedBots(dialogId: Long, bots: List<TL_payments.connectedBotStarRef>) {
        testConnectedBots[dialogId] = bots
        testConnectedBotsUpdateFlow.tryEmit(dialogId)
    }

    fun setTestSuggestedBots(dialogId: Long, bots: List<TL_payments.starRefProgram>) {
        testSuggestedBots[dialogId] = bots
    }

    fun setTestAdminedBots(bots: List<Long>) {
        testAdminedBots.clear()
        testAdminedBots.addAll(bots)
    }

    fun setTestAdminedChannels(channels: List<Long>) {
        testAdminedChannels.clear()
        testAdminedChannels.addAll(channels)
    }

    open fun getController(): BotStarsController? {
        if (isTestMode) return null
        return try {
            BotStarsController.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }
    }

    open fun observeBotStarsUpdated(): Flow<Long?> {
        return try {
            NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.botStarsUpdated)
                .map { event -> if (event.args.isNotEmpty()) event.args[0] as? Long else null }
        } catch (_: Throwable) {
            testBotStarsUpdatedFlow.asSharedFlow()
        }
    }

    open fun observeTransactionsLoaded(): Flow<Long?> {
        return try {
            NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.botStarsTransactionsLoaded)
                .map { event -> if (event.args.isNotEmpty()) event.args[0] as? Long else null }
        } catch (_: Throwable) {
            testTransactionsLoadedFlow.asSharedFlow()
        }
    }

    open fun observeConnectedBotsUpdate(): Flow<Long?> {
        return try {
            NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.channelConnectedBotsUpdate)
                .map { event -> if (event.args.isNotEmpty()) event.args[0] as? Long else null }
        } catch (_: Throwable) {
            testConnectedBotsUpdateFlow.asSharedFlow()
        }
    }

    open fun getStarsRevenueStats(dialogId: Long): TLRPC.TL_payments_starsRevenueStats? {
        testStarsRevenueStats[dialogId]?.let { return it }
        return getController()?.getStarsRevenueStats(dialogId, false)
    }

    open fun saveStarsRevenueStats(dialogId: Long, stats: TLRPC.TL_payments_starsRevenueStats) {
        testStarsRevenueStats[dialogId] = stats
    }

    open fun getTonRevenueStats(dialogId: Long): TLRPC.TL_payments_starsRevenueStats? {
        testTonRevenueStats[dialogId]?.let { return it }
        return getController()?.getTONRevenueStats(dialogId, false)
    }

    open fun saveTonRevenueStats(dialogId: Long, stats: TLRPC.TL_payments_starsRevenueStats) {
        testTonRevenueStats[dialogId] = stats
    }

    open fun getTransactions(dialogId: Long, type: Int): List<TL_stars.StarsTransaction> {
        testTransactions["${dialogId}_$type"]?.let { return it }
        val controller = getController() ?: return emptyList()
        return try {
            controller.getTransactions(dialogId, type) ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun saveTransactions(dialogId: Long, type: Int, txs: List<TL_stars.StarsTransaction>) {
        testTransactions["${dialogId}_$type"] = txs
    }

    open fun getConnectedBots(dialogId: Long): List<TL_payments.connectedBotStarRef> {
        testConnectedBots[dialogId]?.let { return it }
        val controller = getController() ?: return emptyList()
        return try {
            controller.getChannelConnectedBots(dialogId)?.bots ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun saveConnectedBots(dialogId: Long, bots: List<TL_payments.connectedBotStarRef>) {
        testConnectedBots[dialogId] = bots
    }

    open fun getSuggestedBots(dialogId: Long): List<TL_payments.starRefProgram> {
        testSuggestedBots[dialogId]?.let { return it }
        val controller = getController() ?: return emptyList()
        return try {
            controller.getChannelSuggestedBots(dialogId)?.bots ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun saveSuggestedBots(dialogId: Long, bots: List<TL_payments.starRefProgram>) {
        testSuggestedBots[dialogId] = bots
    }

    open fun getAdminedBots(): List<Long> {
        if (testAdminedBots.isNotEmpty()) return testAdminedBots.toList()
        val controller = getController() ?: return emptyList()
        return try {
            controller.adminedBots?.map { it.id } ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun saveAdminedBots(bots: List<Long>) {
        testAdminedBots.clear()
        testAdminedBots.addAll(bots)
    }

    open fun getAdminedChannels(): List<Long> {
        if (testAdminedChannels.isNotEmpty()) return testAdminedChannels.toList()
        val controller = getController() ?: return emptyList()
        return try {
            controller.adminedChannels?.map { it.id } ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun saveAdminedChannels(channels: List<Long>) {
        testAdminedChannels.clear()
        testAdminedChannels.addAll(channels)
    }
}
