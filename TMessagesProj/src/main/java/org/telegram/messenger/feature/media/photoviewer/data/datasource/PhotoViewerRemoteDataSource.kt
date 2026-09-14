package org.telegram.messenger.feature.media.photoviewer.data.datasource

import org.telegram.messenger.ApplicationLoader

class PhotoViewerRemoteDataSource(
    private val currentAccount: Int = 0
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    suspend fun resolveStreamUrl(dialogId: Long, messageId: Long): String? {
        if (!isLegacyAvailable) return null
        return try {
            "https://telegram.org/stream/$dialogId/$messageId"
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun prefetchCloudMedia(dialogId: Long, messageId: Long): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun requestQualityLevels(mediaId: Long): List<Int> {
        if (!isLegacyAvailable) return listOf(360, 480, 720, 1080)
        return try {
            listOf(360, 480, 720, 1080)
        } catch (_: Throwable) {
            listOf(720)
        }
    }
}
