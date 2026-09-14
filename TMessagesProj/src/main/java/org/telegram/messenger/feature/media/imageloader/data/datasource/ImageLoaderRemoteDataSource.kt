package org.telegram.messenger.feature.media.imageloader.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.ImageLoader

class ImageLoaderRemoteDataSource(
    private val currentAccount: Int = 0
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    suspend fun requestRemoteImage(url: String, filter: String?): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            val loader = ImageLoader.getInstance()
            loader != null
        } catch (_: Throwable) {
            true
        }
    }

    suspend fun cancelRemoteImage(url: String): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            val loader = ImageLoader.getInstance()
            loader != null
        } catch (_: Throwable) {
            true
        }
    }
}
