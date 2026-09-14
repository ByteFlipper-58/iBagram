package org.telegram.messenger.feature.media.fileloader.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLoader

class FileLoaderRemoteDataSource(
    private val currentAccount: Int = 0
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    suspend fun requestDownload(
        fileName: String,
        priority: Int = 1,
        size: Long = 0L,
        documentId: Long? = null
    ): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            val loader = FileLoader.getInstance(currentAccount)
            loader != null
        } catch (_: Throwable) {
            true
        }
    }

    suspend fun cancelDownload(fileName: String): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            val loader = FileLoader.getInstance(currentAccount)
            loader?.cancelLoadFile(fileName)
            true
        } catch (_: Throwable) {
            true
        }
    }

    suspend fun cancelAllDownloads(): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            val loader = FileLoader.getInstance(currentAccount)
            loader?.cancelLoadAllFiles()
            true
        } catch (_: Throwable) {
            true
        }
    }

    suspend fun requestUpload(
        filePath: String,
        isEncrypted: Boolean = false,
        isSmall: Boolean = false,
        type: Int = 0
    ): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            val loader = FileLoader.getInstance(currentAccount)
            loader?.uploadFile(filePath, isEncrypted, isSmall, type)
            true
        } catch (_: Throwable) {
            true
        }
    }

    suspend fun cancelUpload(location: String, isEncrypted: Boolean): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            val loader = FileLoader.getInstance(currentAccount)
            loader?.cancelFileUpload(location, isEncrypted)
            true
        } catch (_: Throwable) {
            true
        }
    }

    suspend fun isFileLoading(fileName: String): Boolean {
        if (!isLegacyAvailable) return false
        return try {
            val loader = FileLoader.getInstance(currentAccount)
            loader?.isLoadingFile(fileName) ?: false
        } catch (_: Throwable) {
            false
        }
    }
}
