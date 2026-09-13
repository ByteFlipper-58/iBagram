package org.telegram.messenger.feature.media.chromecast.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.chromecast.ChromecastController
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastMediaModel
import java.io.File

/**
 * Remote/hardware data source for Google Cast session and RemoteMediaClient commands.
 */
open class ChromecastRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    protected open fun getController(): ChromecastController? {
        return try {
            if (ApplicationLoader.applicationContext != null) {
                ChromecastController.getInstance()
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    open fun isCasting(): Boolean {
        return try {
            getController()?.isCasting ?: false
        } catch (_: Throwable) {
            false
        }
    }

    open suspend fun stopCasting(): Result<Unit> {
        return runCatching {
            // Extension point for remote media stop or session release
        }
    }

    open suspend fun setCoverFile(file: File?): Result<String?> {
        return runCatching {
            val controller = getController()
            if (controller != null && file != null) {
                controller.setCover(file)
            } else {
                null
            }
        }
    }
}
