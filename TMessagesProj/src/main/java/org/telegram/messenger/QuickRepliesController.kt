package org.telegram.messenger

import android.os.SystemClock
import androidx.collection.LongSparseArray
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.feature.business.quickreplies.domain.repository.QuickRepliesRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Controller managing quick replies metadata, server query timestamps, and limits.
 * Implemented in 100% Kotlin with full Java interoperability for Phase 4 legacy controller shrinking.
 */
class QuickRepliesController(currentAccount: Int) : BaseController(currentAccount) {

    private val lastQuickReplyServerQueryTime = LongSparseArray<Long>()
    private val queryTimesLock = Any()

    val quickRepliesRepository: QuickRepliesRepository
        get() = getQuickRepliesRepository(currentAccount)

    /**
     * Checks whether quick reply messages for thread [threadMessageId] should be reloaded from server.
     */
    fun shouldReloadMessages(threadMessageId: Long): Boolean {
        synchronized(queryTimesLock) {
            val lastTime = lastQuickReplyServerQueryTime.get(threadMessageId, 0L)
            return (currentElapsedRealtime() - lastTime) > 60 * 1000L
        }
    }

    /**
     * Records the current server query time for quick reply thread [threadMessageId].
     */
    fun recordServerQueryTime(threadMessageId: Long) {
        synchronized(queryTimesLock) {
            lastQuickReplyServerQueryTime.put(threadMessageId, currentElapsedRealtime())
        }
    }

    private fun currentElapsedRealtime(): Long {
        return try {
            SystemClock.elapsedRealtime()
        } catch (e: Throwable) {
            System.currentTimeMillis()
        }
    }

    /**
     * Clears all cached quick reply server query timestamps.
     */
    fun clearQueryTimes() {
        synchronized(queryTimesLock) {
            lastQuickReplyServerQueryTime.clear()
        }
    }

    /**
     * Retrieves the max quick replies limit for the current account.
     */
    val quickRepliesLimit: Int
        get() = messagesController.quickRepliesLimit

    companion object {
        private val instances = ConcurrentHashMap<Int, QuickRepliesController>()

        @JvmStatic
        fun getInstance(account: Int): QuickRepliesController {
            return instances.computeIfAbsent(account) { QuickRepliesController(it) }
        }

        @JvmStatic
        fun getQuickRepliesRepository(account: Int): QuickRepliesRepository {
            return AccountFeatureContainer.get(account).quickRepliesRepository
        }
    }
}
