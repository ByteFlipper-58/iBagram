package org.telegram.messenger.core.events

import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.NotificationCenter

/**
 * Event representation emitted by [NotificationCenterFlowBridge].
 */
data class NotificationEvent(
    val id: Int,
    val account: Int,
    val args: Array<out Any?>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NotificationEvent
        if (id != other.id) return false
        if (account != other.account) return false
        if (!args.contentEquals(other.args)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + account
        result = 31 * result + args.contentHashCode()
        return result
    }
}

/**
 * Reactive bridge transforming legacy [NotificationCenter] observer callbacks into cold Kotlin [Flow]s.
 * Automatically handles registration and unregistration when the collector scope starts and cancels.
 */
object NotificationCenterFlowBridge {

    internal fun runOnMainThread(action: () -> Unit) {
        val isMainThread = try {
            Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper()
        } catch (_: Throwable) {
            false
        }
        if (isMainThread) {
            action()
        } else {
            try {
                AndroidUtilities.runOnUIThread(action)
            } catch (_: Throwable) {
                action()
            }
        }
    }

    /**
     * Observes a specific [eventId] on the [NotificationCenter] for [account].
     */
    fun observeEvent(account: Int, eventId: Int): Flow<NotificationEvent> = callbackFlow {
        val observer = NotificationCenter.NotificationCenterDelegate { id, acc, args ->
            val safeArgs = args ?: emptyArray()
            trySend(NotificationEvent(id, acc, safeArgs))
        }

        runOnMainThread {
            NotificationCenter.getInstance(account).addObserver(observer, eventId)
        }

        awaitClose {
            runOnMainThread {
                NotificationCenter.getInstance(account).removeObserver(observer, eventId)
            }
        }
    }

    /**
     * Observes multiple [eventIds] on the [NotificationCenter] for [account].
     */
    fun observeEvents(account: Int, vararg eventIds: Int): Flow<NotificationEvent> = callbackFlow {
        val observer = NotificationCenter.NotificationCenterDelegate { id, acc, args ->
            val safeArgs = args ?: emptyArray()
            trySend(NotificationEvent(id, acc, safeArgs))
        }

        runOnMainThread {
            val nc = NotificationCenter.getInstance(account)
            for (id in eventIds) {
                nc.addObserver(observer, id)
            }
        }

        awaitClose {
            runOnMainThread {
                val nc = NotificationCenter.getInstance(account)
                for (id in eventIds) {
                    nc.removeObserver(observer, id)
                }
            }
        }
    }

    /**
     * Observes global NotificationCenter events (not account-scoped).
     */
    fun observeGlobalEvent(eventId: Int): Flow<NotificationEvent> = callbackFlow {
        val observer = NotificationCenter.NotificationCenterDelegate { id, acc, args ->
            val safeArgs = args ?: emptyArray()
            trySend(NotificationEvent(id, acc, safeArgs))
        }

        runOnMainThread {
            NotificationCenter.getGlobalInstance().addObserver(observer, eventId)
        }

        awaitClose {
            runOnMainThread {
                NotificationCenter.getGlobalInstance().removeObserver(observer, eventId)
            }
        }
    }
}
