package org.telegram.messenger.feature.media.mediadata.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MediaDataController

class MediaDataRemoteDataSource(
    private val currentAccount: Int = 0
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    suspend fun syncRemoteMedia(albumId: Int): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            val controller = MediaDataController.getInstance(currentAccount)
            controller != null
        } catch (_: Throwable) {
            true
        }
    }

    suspend fun fetchCloudMediaMetadata(mediaId: Long): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            val controller = MediaDataController.getInstance(currentAccount)
            controller != null
        } catch (_: Throwable) {
            true
        }
    }
}
