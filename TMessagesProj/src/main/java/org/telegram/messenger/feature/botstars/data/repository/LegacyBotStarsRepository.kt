package org.telegram.messenger.feature.botstars.data.repository

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withTimeoutOrNull
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.botstars.data.mapper.BotStarsMapper
import org.telegram.messenger.feature.botstars.domain.model.BotStarsRevenueStatsModel
import org.telegram.messenger.feature.botstars.domain.model.BotStarsTransactionModel
import org.telegram.messenger.feature.botstars.domain.model.BotStarsTransactionType
import org.telegram.messenger.feature.botstars.domain.model.ConnectedBotStarRefModel
import org.telegram.messenger.feature.botstars.domain.model.StarRefProgramModel
import org.telegram.messenger.feature.botstars.domain.repository.BotStarsRepository
import org.telegram.ui.Stars.BotStarsController

class LegacyBotStarsRepository(
    private val currentAccount: Int
) : BotStarsRepository {

    private val controller: BotStarsController
        get() = BotStarsController.getInstance(currentAccount)

    private val notificationCenter: NotificationCenter
        get() = NotificationCenter.getInstance(currentAccount)

    override fun observeBotStarsStats(dialogId: Long): Flow<BotStarsRevenueStatsModel?> = callbackFlow {
        val observer = NotificationCenter.NotificationCenterDelegate { id, account, args ->
            if (id == NotificationCenter.botStarsUpdated && account == currentAccount) {
                val updatedDid = args.getOrNull(0) as? Long
                if (updatedDid == null || updatedDid == dialogId) {
                    val stats = controller.getStarsRevenueStats(dialogId)
                    trySend(BotStarsMapper.toRevenueStats(dialogId, stats))
                }
            }
        }

        notificationCenter.addObserver(observer, NotificationCenter.botStarsUpdated)

        val initial = controller.getStarsRevenueStats(dialogId)
        if (initial != null) {
            trySend(BotStarsMapper.toRevenueStats(dialogId, initial))
        } else {
            AndroidUtilities.runOnUIThread {
                controller.getStarsRevenueStats(dialogId, true)
            }
        }

        awaitClose {
            notificationCenter.removeObserver(observer, NotificationCenter.botStarsUpdated)
        }
    }

    override suspend fun getBotStarsStats(dialogId: Long, force: Boolean): Result<BotStarsRevenueStatsModel?> {
        val cached = controller.getStarsRevenueStats(dialogId)
        if (!force && cached != null) {
            return Result.Success(BotStarsMapper.toRevenueStats(dialogId, cached))
        }

        return withTimeoutOrNull(10000L) {
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                val observer = object : NotificationCenter.NotificationCenterDelegate {
                    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
                        if (id == NotificationCenter.botStarsUpdated && account == currentAccount) {
                            val updatedDid = args.getOrNull(0) as? Long
                            if (updatedDid == null || updatedDid == dialogId) {
                                notificationCenter.removeObserver(this, NotificationCenter.botStarsUpdated)
                                val stats = controller.getStarsRevenueStats(dialogId)
                                if (continuation.isActive) {
                                    continuation.resumeWith(
                                        kotlin.Result.success(
                                            Result.Success(BotStarsMapper.toRevenueStats(dialogId, stats))
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                notificationCenter.addObserver(observer, NotificationCenter.botStarsUpdated)
                continuation.invokeOnCancellation {
                    notificationCenter.removeObserver(observer, NotificationCenter.botStarsUpdated)
                }

                AndroidUtilities.runOnUIThread {
                    controller.getStarsRevenueStats(dialogId, force)
                }
            }
        } ?: Result.Success(BotStarsMapper.toRevenueStats(dialogId, controller.getStarsRevenueStats(dialogId)))
    }

    override fun observeTonStats(dialogId: Long): Flow<BotStarsRevenueStatsModel?> = callbackFlow {
        val observer = NotificationCenter.NotificationCenterDelegate { id, account, args ->
            if (id == NotificationCenter.botStarsUpdated && account == currentAccount) {
                val updatedDid = args.getOrNull(0) as? Long
                if (updatedDid == null || updatedDid == dialogId) {
                    val stats = controller.getTONRevenueStats(dialogId, false)
                    trySend(BotStarsMapper.toRevenueStats(dialogId, stats))
                }
            }
        }

        notificationCenter.addObserver(observer, NotificationCenter.botStarsUpdated)

        val initial = controller.getTONRevenueStats(dialogId, false)
        if (initial != null) {
            trySend(BotStarsMapper.toRevenueStats(dialogId, initial))
        } else {
            AndroidUtilities.runOnUIThread {
                controller.getTONRevenueStats(dialogId, true)
            }
        }

        awaitClose {
            notificationCenter.removeObserver(observer, NotificationCenter.botStarsUpdated)
        }
    }

    override suspend fun getTonStats(dialogId: Long, force: Boolean): Result<BotStarsRevenueStatsModel?> {
        val cached = controller.getTONRevenueStats(dialogId, false)
        if (!force && cached != null) {
            return Result.Success(BotStarsMapper.toRevenueStats(dialogId, cached))
        }

        return withTimeoutOrNull(10000L) {
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                val observer = object : NotificationCenter.NotificationCenterDelegate {
                    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
                        if (id == NotificationCenter.botStarsUpdated && account == currentAccount) {
                            val updatedDid = args.getOrNull(0) as? Long
                            if (updatedDid == null || updatedDid == dialogId) {
                                notificationCenter.removeObserver(this, NotificationCenter.botStarsUpdated)
                                val stats = controller.getTONRevenueStats(dialogId, false)
                                if (continuation.isActive) {
                                    continuation.resumeWith(
                                        kotlin.Result.success(
                                            Result.Success(BotStarsMapper.toRevenueStats(dialogId, stats))
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                notificationCenter.addObserver(observer, NotificationCenter.botStarsUpdated)
                continuation.invokeOnCancellation {
                    notificationCenter.removeObserver(observer, NotificationCenter.botStarsUpdated)
                }

                AndroidUtilities.runOnUIThread {
                    controller.getTONRevenueStats(dialogId, force)
                }
            }
        } ?: Result.Success(BotStarsMapper.toRevenueStats(dialogId, controller.getTONRevenueStats(dialogId, false)))
    }

    override fun observeTransactions(
        dialogId: Long,
        type: BotStarsTransactionType
    ): Flow<List<BotStarsTransactionModel>> = callbackFlow {
        val observer = NotificationCenter.NotificationCenterDelegate { id, account, args ->
            if (id == NotificationCenter.botStarsTransactionsLoaded && account == currentAccount) {
                val updatedDid = args.getOrNull(0) as? Long
                if (updatedDid == null || updatedDid == dialogId) {
                    val txs = controller.getTransactions(dialogId, type.legacyId)
                    trySend(BotStarsMapper.toTransactionList(txs))
                }
            }
        }

        notificationCenter.addObserver(observer, NotificationCenter.botStarsTransactionsLoaded)

        val currentTxs = controller.getTransactions(dialogId, type.legacyId)
        if (!currentTxs.isEmpty()) {
            trySend(BotStarsMapper.toTransactionList(currentTxs))
        } else {
            AndroidUtilities.runOnUIThread {
                controller.loadTransactions(dialogId, type.legacyId)
            }
        }

        awaitClose {
            notificationCenter.removeObserver(observer, NotificationCenter.botStarsTransactionsLoaded)
        }
    }

    override suspend fun loadTransactions(
        dialogId: Long,
        type: BotStarsTransactionType,
        reload: Boolean
    ): Result<List<BotStarsTransactionModel>> {
        val currentTxs = controller.getTransactions(dialogId, type.legacyId)
        if (!reload && !currentTxs.isEmpty()) {
            return Result.Success(BotStarsMapper.toTransactionList(currentTxs))
        }

        return withTimeoutOrNull(10000L) {
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                val observer = object : NotificationCenter.NotificationCenterDelegate {
                    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
                        if (id == NotificationCenter.botStarsTransactionsLoaded && account == currentAccount) {
                            val updatedDid = args.getOrNull(0) as? Long
                            if (updatedDid == null || updatedDid == dialogId) {
                                notificationCenter.removeObserver(this, NotificationCenter.botStarsTransactionsLoaded)
                                val txs = controller.getTransactions(dialogId, type.legacyId)
                                if (continuation.isActive) {
                                    continuation.resumeWith(
                                        kotlin.Result.success(
                                            Result.Success(BotStarsMapper.toTransactionList(txs))
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                notificationCenter.addObserver(observer, NotificationCenter.botStarsTransactionsLoaded)
                continuation.invokeOnCancellation {
                    notificationCenter.removeObserver(observer, NotificationCenter.botStarsTransactionsLoaded)
                }

                AndroidUtilities.runOnUIThread {
                    if (reload) {
                        controller.invalidateTransactions(dialogId, true)
                    } else {
                        controller.loadTransactions(dialogId, type.legacyId)
                    }
                }
            }
        } ?: Result.Success(BotStarsMapper.toTransactionList(controller.getTransactions(dialogId, type.legacyId)))
    }

    override fun observeConnectedBots(dialogId: Long): Flow<List<ConnectedBotStarRefModel>> = callbackFlow {
        val observer = NotificationCenter.NotificationCenterDelegate { id, account, args ->
            if (id == NotificationCenter.channelConnectedBotsUpdate && account == currentAccount) {
                val updatedDid = args.getOrNull(0) as? Long
                if (updatedDid == null || updatedDid == dialogId) {
                    val channelBots = controller.getChannelConnectedBots(dialogId)
                    trySend(BotStarsMapper.toConnectedBotList(channelBots.bots))
                }
            }
        }

        notificationCenter.addObserver(observer, NotificationCenter.channelConnectedBotsUpdate)

        val channelBots = controller.getChannelConnectedBots(dialogId)
        if (!channelBots.bots.isEmpty()) {
            trySend(BotStarsMapper.toConnectedBotList(channelBots.bots))
        } else {
            AndroidUtilities.runOnUIThread {
                channelBots.load()
            }
        }

        awaitClose {
            notificationCenter.removeObserver(observer, NotificationCenter.channelConnectedBotsUpdate)
        }
    }

    override suspend fun loadConnectedBots(
        dialogId: Long,
        reload: Boolean
    ): Result<List<ConnectedBotStarRefModel>> {
        val channelBots = controller.getChannelConnectedBots(dialogId)
        if (!reload && !channelBots.bots.isEmpty()) {
            return Result.Success(BotStarsMapper.toConnectedBotList(channelBots.bots))
        }

        return withTimeoutOrNull(10000L) {
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                val observer = object : NotificationCenter.NotificationCenterDelegate {
                    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
                        if (id == NotificationCenter.channelConnectedBotsUpdate && account == currentAccount) {
                            val updatedDid = args.getOrNull(0) as? Long
                            if (updatedDid == null || updatedDid == dialogId) {
                                notificationCenter.removeObserver(this, NotificationCenter.channelConnectedBotsUpdate)
                                if (continuation.isActive) {
                                    continuation.resumeWith(
                                        kotlin.Result.success(
                                            Result.Success(BotStarsMapper.toConnectedBotList(channelBots.bots))
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                notificationCenter.addObserver(observer, NotificationCenter.channelConnectedBotsUpdate)
                continuation.invokeOnCancellation {
                    notificationCenter.removeObserver(observer, NotificationCenter.channelConnectedBotsUpdate)
                }

                AndroidUtilities.runOnUIThread {
                    if (reload) {
                        channelBots.clear()
                        channelBots.cancel()
                    }
                    channelBots.load()
                }
            }
        } ?: Result.Success(BotStarsMapper.toConnectedBotList(channelBots.bots))
    }

    override suspend fun loadSuggestedBots(
        dialogId: Long,
        sort: Int
    ): Result<List<StarRefProgramModel>> {
        val suggested = controller.getChannelSuggestedBots(dialogId)
        return withTimeoutOrNull(10000L) {
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                val observer = object : NotificationCenter.NotificationCenterDelegate {
                    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
                        if (id == NotificationCenter.channelSuggestedBotsUpdate && account == currentAccount) {
                            val updatedDid = args.getOrNull(0) as? Long
                            if (updatedDid == null || updatedDid == dialogId) {
                                notificationCenter.removeObserver(this, NotificationCenter.channelSuggestedBotsUpdate)
                                if (continuation.isActive) {
                                    continuation.resumeWith(
                                        kotlin.Result.success(
                                            Result.Success(BotStarsMapper.toSuggestedBotList(suggested.bots))
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                notificationCenter.addObserver(observer, NotificationCenter.channelSuggestedBotsUpdate)
                continuation.invokeOnCancellation {
                    notificationCenter.removeObserver(observer, NotificationCenter.channelSuggestedBotsUpdate)
                }

                AndroidUtilities.runOnUIThread {
                    val sortEnum = when (sort) {
                        1 -> BotStarsController.ChannelSuggestedBots.Sort.BY_REVENUE
                        2 -> BotStarsController.ChannelSuggestedBots.Sort.BY_DATE
                        else -> BotStarsController.ChannelSuggestedBots.Sort.BY_PROFITABILITY
                    }
                    suggested.setSort(sortEnum)
                    suggested.load()
                }
            }
        } ?: Result.Success(BotStarsMapper.toSuggestedBotList(suggested.bots))
    }

    override suspend fun loadAdminedBots(): Result<List<Long>> {
        val cached = controller.adminedBots
        if (cached != null) {
            return Result.Success(cached.map { it.id })
        }

        return withTimeoutOrNull(10000L) {
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                AndroidUtilities.runOnUIThread {
                    controller.loadAdminedBots()
                }
                // Poll check or short delay on main thread
                AndroidUtilities.runOnUIThread {
                    val list = controller.adminedBots?.map { it.id } ?: emptyList()
                    continuation.resumeWith(kotlin.Result.success(Result.Success(list)))
                }
            }
        } ?: Result.Success(emptyList())
    }

    override suspend fun loadAdminedChannels(): Result<List<Long>> {
        val cached = controller.adminedChannels
        if (cached != null) {
            return Result.Success(cached.map { it.id })
        }

        return withTimeoutOrNull(10000L) {
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                val observer = object : NotificationCenter.NotificationCenterDelegate {
                    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
                        if (id == NotificationCenter.adminedChannelsLoaded && account == currentAccount) {
                            notificationCenter.removeObserver(this, NotificationCenter.adminedChannelsLoaded)
                            val list = controller.adminedChannels?.map { it.id } ?: emptyList()
                            if (continuation.isActive) {
                                continuation.resumeWith(
                                    kotlin.Result.success(Result.Success(list))
                                )
                            }
                        }
                    }
                }

                notificationCenter.addObserver(observer, NotificationCenter.adminedChannelsLoaded)
                continuation.invokeOnCancellation {
                    notificationCenter.removeObserver(observer, NotificationCenter.adminedChannelsLoaded)
                }

                AndroidUtilities.runOnUIThread {
                    controller.loadAdminedChannels()
                }
            }
        } ?: Result.Success(controller.adminedChannels?.map { it.id } ?: emptyList())
    }
}
