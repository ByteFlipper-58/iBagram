package org.telegram.messenger.feature.security.unconfirmedauth.data.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UnconfirmedAuthController
import org.telegram.messenger.feature.security.unconfirmedauth.data.mapper.UnconfirmedAuthMapper
import org.telegram.messenger.feature.security.unconfirmedauth.domain.model.UnconfirmedAuthModel
import org.telegram.messenger.feature.security.unconfirmedauth.domain.model.UnconfirmedAuthStateModel
import java.util.ArrayList

/**
 * Local data source encapsulating SQLite storage and in-memory caching for unconfirmed authorizations.
 */
class UnconfirmedAuthLocalDataSource(
    private val currentAccount: Int,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val inMemoryFallback = ArrayList<UnconfirmedAuthModel>()

    private fun getController(): UnconfirmedAuthController? {
        return try {
            MessagesController.getInstance(currentAccount)?.unconfirmedAuthController
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun getCachedAuths(): UnconfirmedAuthStateModel = withContext(ioDispatcher) {
        val ctrl = getController()
        if (ctrl != null) {
            UnconfirmedAuthMapper.mapState(ctrl.auths)
        } else {
            UnconfirmedAuthStateModel(
                auths = inMemoryFallback.toList(),
                isLoading = false
            )
        }
    }

    suspend fun findAuth(hash: Long): Pair<UnconfirmedAuthController.UnconfirmedAuth?, UnconfirmedAuthModel?> =
        withContext(ioDispatcher) {
            val ctrl = getController()
            if (ctrl != null) {
                val found = ctrl.auths.firstOrNull { it.hash == hash }
                Pair(found, null)
            } else {
                val found = inMemoryFallback.firstOrNull { it.hash == hash }
                Pair(null, found)
            }
        }

    suspend fun removeAuth(hash: Long): Boolean = withContext(ioDispatcher) {
        val ctrl = getController()
        if (ctrl != null) {
            var removed = false
            for (i in 0 until ctrl.auths.size) {
                if (ctrl.auths[i]?.hash == hash) {
                    ctrl.auths.removeAt(i)
                    removed = true
                    break
                }
            }
            if (removed) {
                ctrl.saveCache()
            }
            removed
        } else {
            inMemoryFallback.removeAll { it.hash == hash }
        }
    }

    suspend fun removeAuths(hashes: Set<Long>): Int = withContext(ioDispatcher) {
        val ctrl = getController()
        if (ctrl != null) {
            var count = 0
            val iterator = ctrl.auths.iterator()
            while (iterator.hasNext()) {
                val auth = iterator.next()
                if (auth != null && hashes.contains(auth.hash)) {
                    iterator.remove()
                    count++
                }
            }
            if (count > 0) {
                ctrl.saveCache()
            }
            count
        } else {
            val before = inMemoryFallback.size
            inMemoryFallback.removeAll { hashes.contains(it.hash) }
            before - inMemoryFallback.size
        }
    }

    suspend fun clearAll(): Unit = withContext(ioDispatcher) {
        val ctrl = getController()
        if (ctrl != null) {
            ctrl.cleanup()
        } else {
            inMemoryFallback.clear()
        }
    }

    fun addInMemoryFallback(auth: UnconfirmedAuthModel) {
        inMemoryFallback.removeAll { it.hash == auth.hash }
        inMemoryFallback.add(auth)
    }
}
