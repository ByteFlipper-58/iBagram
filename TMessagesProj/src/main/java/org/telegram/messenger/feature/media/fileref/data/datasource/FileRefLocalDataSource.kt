package org.telegram.messenger.feature.media.fileref.data.datasource

import android.util.Pair
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.FileRefController
import org.telegram.tgnet.TLRPC

/**
 * Local data source managing file reference cache, parent object keys,
 * and interactions with legacy [FileRefController].
 */
open class FileRefLocalDataSource(
    protected val currentAccount: Int,
    protected val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // Fallback memory cache for headless and unit test environments
    private val memoryParentKeys = mutableMapOf<Any, String>()
    private val memoryCachedReferences = mutableMapOf<String, ByteArray>()

    protected open fun getFileRefController(): FileRefController? {
        return try {
            FileRefController.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }
    }

    open fun isFileRefError(error: String?): Boolean {
        if (error == null) return false
        return try {
            FileRefController.isFileRefError(error)
        } catch (_: Throwable) {
            error.startsWith("FILE_REFERENCE_")
        }
    }

    open fun getKeyForParentObject(parentObject: Any?): String? {
        if (parentObject == null) return null
        return try {
            FileRefController.getKeyForParentObject(parentObject) ?: memoryParentKeys[parentObject]
        } catch (_: Throwable) {
            memoryParentKeys[parentObject]
        }
    }

    open fun registerParentKeyInMemory(parentObject: Any, key: String) {
        memoryParentKeys[parentObject] = key
    }

    open fun applyCachedFileReference(parentObject: Any, vararg args: Any?): Boolean {
        return try {
            val controller = getFileRefController()
            if (controller != null) {
                controller.applyCachedFileReference(parentObject, *args)
            } else {
                val key = getKeyForParentObject(parentObject)
                key != null && memoryCachedReferences.containsKey(key)
            }
        } catch (_: Throwable) {
            val key = getKeyForParentObject(parentObject)
            key != null && memoryCachedReferences.containsKey(key)
        }
    }

    open fun requestReference(parentObject: Any, vararg args: Any?) {
        try {
            getFileRefController()?.requestReference(parentObject, *args)
        } catch (_: Throwable) {
        }
    }

    open fun putCachedReferenceInMemory(parentKey: String, ref: ByteArray) {
        memoryCachedReferences[parentKey] = ref
    }

    open fun clearMemoryCache() {
        memoryParentKeys.clear()
        memoryCachedReferences.clear()
    }
}
