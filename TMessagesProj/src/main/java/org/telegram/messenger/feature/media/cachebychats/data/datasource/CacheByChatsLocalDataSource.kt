package org.telegram.messenger.feature.media.cachebychats.data.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.CacheByChatsController
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.UserConfig

/**
 * Local data source managing cache retention periods, exceptions persistence,
 * and interactions with [CacheByChatsController].
 */
open class CacheByChatsLocalDataSource(
    protected val currentAccount: Int,
    protected val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // Fallback in-memory storage for headless/unit-test environments
    private val memoryKeepMedia = mutableMapOf<Int, Int>()
    private val memoryExceptions = mutableMapOf<Int, ArrayList<CacheByChatsController.KeepMediaException>>()

    protected open fun getController(): CacheByChatsController? {
        return try {
            CacheByChatsController(currentAccount)
        } catch (_: Throwable) {
            null
        }
    }

    open fun getKeepMedia(type: Int): Int {
        return try {
            val controller = getController()
            if (controller != null) {
                controller.getKeepMedia(type)
            } else {
                memoryKeepMedia[type] ?: CacheByChatsController.getDefault(type)
            }
        } catch (_: Throwable) {
            memoryKeepMedia[type] ?: CacheByChatsController.getDefault(type)
        }
    }

    open fun setKeepMedia(type: Int, keepMedia: Int) {
        try {
            val controller = getController()
            if (controller != null) {
                controller.setKeepMedia(type, keepMedia)
            } else {
                memoryKeepMedia[type] = keepMedia
            }
        } catch (_: Throwable) {
            memoryKeepMedia[type] = keepMedia
        }
    }

    open fun getKeepMediaExceptions(type: Int): ArrayList<CacheByChatsController.KeepMediaException> {
        return try {
            val controller = getController()
            if (controller != null) {
                controller.getKeepMediaExceptions(type) ?: ArrayList()
            } else {
                memoryExceptions[type] ?: ArrayList()
            }
        } catch (_: Throwable) {
            memoryExceptions[type] ?: ArrayList()
        }
    }

    open fun saveKeepMediaExceptions(type: Int, exceptions: ArrayList<CacheByChatsController.KeepMediaException>) {
        try {
            val controller = getController()
            if (controller != null) {
                controller.saveKeepMediaExceptions(type, exceptions)
            } else {
                memoryExceptions[type] = ArrayList(exceptions)
            }
        } catch (_: Throwable) {
            memoryExceptions[type] = ArrayList(exceptions)
        }
    }
}
