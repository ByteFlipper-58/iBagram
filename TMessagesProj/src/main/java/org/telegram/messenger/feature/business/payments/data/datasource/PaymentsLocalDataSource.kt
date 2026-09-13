package org.telegram.messenger.feature.business.payments.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.tgnet.tl.TL_stars
import org.telegram.ui.Stars.StarsController
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Local data source managing cached Stars balance, transaction history, subscriptions, and top-up options.
 */
open class PaymentsLocalDataSource(
    private val currentAccount: Int
) {
    private var isTestMode = false

    private var testBalance: Long? = null
    private var testBalanceAvailable: Boolean? = null
    private val testTransactions = ConcurrentHashMap<Int, List<TL_stars.StarsTransaction>>()
    private val testSubscriptions = CopyOnWriteArrayList<TL_stars.StarsSubscription>()
    private val testTopupOptions = CopyOnWriteArrayList<TL_stars.TL_starsTopupOption>()

    private val testBalanceFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    private val testTransactionsFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    private val testSubscriptionsFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    fun setTestMode(testMode: Boolean) {
        this.isTestMode = testMode
    }

    fun setTestBalance(balance: Long, isAvailable: Boolean) {
        testBalance = balance
        testBalanceAvailable = isAvailable
        testBalanceFlow.tryEmit(Unit)
    }

    fun setTestTransactions(type: Int, txs: List<TL_stars.StarsTransaction>) {
        testTransactions[type] = txs
        testTransactionsFlow.tryEmit(Unit)
    }

    fun setTestSubscriptions(subs: List<TL_stars.StarsSubscription>) {
        testSubscriptions.clear()
        testSubscriptions.addAll(subs)
        testSubscriptionsFlow.tryEmit(Unit)
    }

    fun setTestTopupOptions(opts: List<TL_stars.TL_starsTopupOption>) {
        testTopupOptions.clear()
        testTopupOptions.addAll(opts)
    }

    open fun getController(): StarsController? {
        if (isTestMode) return null
        return try {
            StarsController.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }
    }

    open fun observeBalanceUpdated(): Flow<Unit> {
        return try {
            NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.starBalanceUpdated)
                .map { Unit }
        } catch (_: Throwable) {
            testBalanceFlow.asSharedFlow()
        }
    }

    open fun observeTransactionsUpdated(): Flow<Unit> {
        return try {
            NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.starTransactionsLoaded)
                .map { Unit }
        } catch (_: Throwable) {
            testTransactionsFlow.asSharedFlow()
        }
    }

    open fun observeSubscriptionsUpdated(): Flow<Unit> {
        return try {
            NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.starSubscriptionsLoaded)
                .map { Unit }
        } catch (_: Throwable) {
            testSubscriptionsFlow.asSharedFlow()
        }
    }

    open fun getBalance(): Long {
        testBalance?.let { return it }
        val controller = getController() ?: return 0L
        return try {
            controller.getBalance(false)
        } catch (_: Throwable) {
            0L
        }
    }

    open fun isBalanceAvailable(): Boolean {
        testBalanceAvailable?.let { return it }
        val controller = getController() ?: return false
        return try {
            controller.balanceAvailable()
        } catch (_: Throwable) {
            false
        }
    }

    open fun saveBalance(balance: Long, isAvailable: Boolean) {
        testBalance = balance
        testBalanceAvailable = isAvailable
    }

    open fun getTransactions(type: Int): List<TL_stars.StarsTransaction> {
        testTransactions[type]?.let { return it }
        val controller = getController() ?: return emptyList()
        return try {
            val safeType = if (type in 0..2) type else StarsController.ALL_TRANSACTIONS
            controller.transactions[safeType] ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun saveTransactions(type: Int, txs: List<TL_stars.StarsTransaction>) {
        testTransactions[type] = txs
    }

    open fun getSubscriptions(): List<TL_stars.StarsSubscription> {
        if (testSubscriptions.isNotEmpty()) return testSubscriptions.toList()
        val controller = getController() ?: return emptyList()
        return try {
            controller.subscriptions ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun saveSubscriptions(subs: List<TL_stars.StarsSubscription>) {
        testSubscriptions.clear()
        testSubscriptions.addAll(subs)
    }

    open fun getTopupOptions(): List<TL_stars.TL_starsTopupOption> {
        if (testTopupOptions.isNotEmpty()) return testTopupOptions.toList()
        val controller = getController() ?: return emptyList()
        return try {
            controller.options ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun saveTopupOptions(opts: List<TL_stars.TL_starsTopupOption>) {
        testTopupOptions.clear()
        testTopupOptions.addAll(opts)
    }
}
