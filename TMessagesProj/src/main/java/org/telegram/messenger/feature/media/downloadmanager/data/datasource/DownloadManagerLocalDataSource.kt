package org.telegram.messenger.feature.media.downloadmanager.data.datasource

import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.telegram.messenger.DownloadController
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.core.data.BaseLocalDataSource
import org.telegram.messenger.core.result.Result
import java.util.ArrayList

/**
 * Local data source managing auto-download settings persistence in SharedPreferences,
 * SQLite storage via MessagesStorage on Dispatchers.IO, and in-memory caches of DownloadController.
 */
open class DownloadManagerLocalDataSource(
    currentAccount: Int,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseLocalDataSource(currentAccount, ioDispatcher) {

    private val safeDownloadController: DownloadController?
        get() = try {
            DownloadController.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }

    private val safeStorage: MessagesStorage?
        get() = try {
            MessagesStorage.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }

    // --- SharedPreferences Access ---

    open fun getPreferences(): SharedPreferences? {
        return try {
            MessagesController.getMainSettings(currentAccount)
        } catch (_: Throwable) {
            null
        }
    }

    open fun getPresetString(key: String, defaultValue: String = ""): String {
        return getPreferences()?.getString(key, defaultValue) ?: defaultValue
    }

    open fun savePresetString(key: String, value: String) {
        getPreferences()?.edit()?.putString(key, value)?.apply()
    }

    // --- SQLite Database Operations ---

    open suspend fun clearRecentDownloadedDocuments(): Result<Unit> {
        val storage = safeStorage ?: return Result.success(Unit)
        return runOnDb {
            try {
                storage.database.executeFast("DELETE FROM downloading_documents WHERE state = 1").stepThis().dispose()
            } catch (e: Exception) {
                // Ignore table absence or database locked errors
            }
        }
    }

    open suspend fun deleteRecentDocument(dcId: Int, id: Long): Result<Unit> {
        val storage = safeStorage ?: return Result.success(Unit)
        return runOnDb {
            try {
                val state = storage.database.executeFast("DELETE FROM downloading_documents WHERE hash = ? AND id = ?")
                state.bindInteger(1, dcId)
                state.bindLong(2, id)
                state.step()
                state.dispose()
            } catch (e: Exception) {
                // Ignore table absence or database locked errors
            }
        }
    }

    // --- DownloadController Delegations & In-Memory Queues ---

    open fun getDownloadingFiles(): List<MessageObject> {
        val controller = safeDownloadController ?: return emptyList()
        return synchronized(controller.downloadingFiles) {
            ArrayList(controller.downloadingFiles)
        }
    }

    open fun getRecentDownloadingFiles(): List<MessageObject> {
        val controller = safeDownloadController ?: return emptyList()
        return synchronized(controller.recentDownloadingFiles) {
            ArrayList(controller.recentDownloadingFiles)
        }
    }

    open fun getUnviewedDownloads(): List<MessageObject> {
        val controller = safeDownloadController ?: return emptyList()
        val list = ArrayList<MessageObject>()
        for (i in 0 until controller.unviewedDownloads.size()) {
            list.add(controller.unviewedDownloads.valueAt(i))
        }
        return list
    }

    open fun clearRecentFiles() {
        safeDownloadController?.clearRecentDownloadedFiles()
    }

    open fun deleteRecentFiles(messageObjects: ArrayList<MessageObject>) {
        safeDownloadController?.deleteRecentFiles(messageObjects)
    }

    open fun checkAutodownloadSettings() {
        safeDownloadController?.checkAutodownloadSettings()
    }

    open fun canDownloadMedia(type: Int, size: Long): Boolean {
        return safeDownloadController?.canDownloadMedia(type, size) ?: true
    }
}
