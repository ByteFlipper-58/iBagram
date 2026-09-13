package org.telegram.messenger.feature.business.botstars.data.datasource

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_payments
import org.telegram.tgnet.tl.TL_stars
import org.telegram.ui.Stars.BotStarsController
import kotlin.coroutines.resume

/**
 * Remote data source for requesting Bot Stars statistics, transactions, and referral data.
 */
open class BotStarsRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun fetchBotStarsStats(
        dialogId: Long,
        force: Boolean = false
    ): Result<TLRPC.TL_payments_starsRevenueStats?> {
        return try {
            val controller = BotStarsController.getInstance(currentAccount)
            val notificationCenter = NotificationCenter.getInstance(currentAccount)
            withTimeoutOrNull(10000L) {
                suspendCancellableCoroutine { continuation ->
                    val observer = object : NotificationCenter.NotificationCenterDelegate {
                        override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
                            if (id == NotificationCenter.botStarsUpdated && account == currentAccount) {
                                val updatedDid = args.getOrNull(0) as? Long
                                if (updatedDid == null || updatedDid == dialogId) {
                                    notificationCenter.removeObserver(this, NotificationCenter.botStarsUpdated)
                                    val stats = controller.getStarsRevenueStats(dialogId)
                                    if (continuation.isActive) {
                                        continuation.resume(Result.Success(stats))
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
            } ?: Result.Success(controller.getStarsRevenueStats(dialogId))
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to fetch bot stars stats", e))
        }
    }

    open suspend fun fetchTonStats(
        dialogId: Long,
        force: Boolean = false
    ): Result<TLRPC.TL_payments_starsRevenueStats?> {
        return try {
            val controller = BotStarsController.getInstance(currentAccount)
            val notificationCenter = NotificationCenter.getInstance(currentAccount)
            withTimeoutOrNull(10000L) {
                suspendCancellableCoroutine { continuation ->
                    val observer = object : NotificationCenter.NotificationCenterDelegate {
                        override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
                            if (id == NotificationCenter.botStarsUpdated && account == currentAccount) {
                                val updatedDid = args.getOrNull(0) as? Long
                                if (updatedDid == null || updatedDid == dialogId) {
                                    notificationCenter.removeObserver(this, NotificationCenter.botStarsUpdated)
                                    val stats = controller.getTONRevenueStats(dialogId, false)
                                    if (continuation.isActive) {
                                        continuation.resume(Result.Success(stats))
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
            } ?: Result.Success(controller.getTONRevenueStats(dialogId, false))
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to fetch TON revenue stats", e))
        }
    }

    open suspend fun fetchTransactions(
        dialogId: Long,
        type: Int,
        reload: Boolean = false
    ): Result<List<TL_stars.StarsTransaction>> {
        return try {
            val controller = BotStarsController.getInstance(currentAccount)
            val notificationCenter = NotificationCenter.getInstance(currentAccount)
            withTimeoutOrNull(10000L) {
                suspendCancellableCoroutine { continuation ->
                    val observer = object : NotificationCenter.NotificationCenterDelegate {
                        override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
                            if (id == NotificationCenter.botStarsTransactionsLoaded && account == currentAccount) {
                                val updatedDid = args.getOrNull(0) as? Long
                                if (updatedDid == null || updatedDid == dialogId) {
                                    notificationCenter.removeObserver(this, NotificationCenter.botStarsTransactionsLoaded)
                                    val txs = controller.getTransactions(dialogId, type) ?: emptyList()
                                    if (continuation.isActive) {
                                        continuation.resume(Result.Success(txs))
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
                            controller.loadTransactions(dialogId, type)
                        }
                    }
                }
            } ?: Result.Success(controller.getTransactions(dialogId, type) ?: emptyList())
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to fetch bot transactions", e))
        }
    }

    open suspend fun fetchConnectedBots(
        dialogId: Long,
        reload: Boolean = false
    ): Result<List<TL_payments.connectedBotStarRef>> {
        return try {
            val controller = BotStarsController.getInstance(currentAccount)
            val channelBots = controller.getChannelConnectedBots(dialogId)
            val notificationCenter = NotificationCenter.getInstance(currentAccount)
            withTimeoutOrNull(10000L) {
                suspendCancellableCoroutine { continuation ->
                    val observer = object : NotificationCenter.NotificationCenterDelegate {
                        override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
                            if (id == NotificationCenter.channelConnectedBotsUpdate && account == currentAccount) {
                                val updatedDid = args.getOrNull(0) as? Long
                                if (updatedDid == null || updatedDid == dialogId) {
                                    notificationCenter.removeObserver(this, NotificationCenter.channelConnectedBotsUpdate)
                                    if (continuation.isActive) {
                                        continuation.resume(Result.Success(channelBots.bots ?: emptyList()))
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
            } ?: Result.Success(channelBots.bots ?: emptyList())
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to fetch connected bots", e))
        }
    }

    open suspend fun fetchSuggestedBots(
        dialogId: Long,
        sort: Int
    ): Result<List<TL_payments.starRefProgram>> {
        return try {
            val controller = BotStarsController.getInstance(currentAccount)
            val suggested = controller.getChannelSuggestedBots(dialogId)
            val notificationCenter = NotificationCenter.getInstance(currentAccount)
            withTimeoutOrNull(10000L) {
                suspendCancellableCoroutine { continuation ->
                    val observer = object : NotificationCenter.NotificationCenterDelegate {
                        override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
                            if (id == NotificationCenter.channelSuggestedBotsUpdate && account == currentAccount) {
                                val updatedDid = args.getOrNull(0) as? Long
                                if (updatedDid == null || updatedDid == dialogId) {
                                    notificationCenter.removeObserver(this, NotificationCenter.channelSuggestedBotsUpdate)
                                    if (continuation.isActive) {
                                        continuation.resume(Result.Success(suggested.bots ?: emptyList()))
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
            } ?: Result.Success(suggested.bots ?: emptyList())
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to fetch suggested bots", e))
        }
    }

    open suspend fun fetchAdminedBots(): Result<List<Long>> {
        return try {
            val controller = BotStarsController.getInstance(currentAccount)
            val cached = controller.adminedBots
            if (cached != null) {
                return Result.Success(cached.map { it.id })
            }
            withTimeoutOrNull(10000L) {
                suspendCancellableCoroutine { continuation ->
                    AndroidUtilities.runOnUIThread {
                        controller.loadAdminedBots()
                    }
                    AndroidUtilities.runOnUIThread {
                        val list = controller.adminedBots?.map { it.id } ?: emptyList()
                        continuation.resume(Result.Success(list))
                    }
                }
            } ?: Result.Success(controller.adminedBots?.map { it.id } ?: emptyList())
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to fetch admined bots", e))
        }
    }

    open suspend fun fetchAdminedChannels(): Result<List<Long>> {
        return try {
            val controller = BotStarsController.getInstance(currentAccount)
            val cached = controller.adminedChannels
            if (cached != null) {
                return Result.Success(cached.map { it.id })
            }
            val notificationCenter = NotificationCenter.getInstance(currentAccount)
            withTimeoutOrNull(10000L) {
                suspendCancellableCoroutine { continuation ->
                    val observer = object : NotificationCenter.NotificationCenterDelegate {
                        override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
                            if (id == NotificationCenter.adminedChannelsLoaded && account == currentAccount) {
                                notificationCenter.removeObserver(this, NotificationCenter.adminedChannelsLoaded)
                                val list = controller.adminedChannels?.map { it.id } ?: emptyList()
                                if (continuation.isActive) {
                                    continuation.resume(Result.Success(list))
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
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to fetch admined channels", e))
        }
    }
}
