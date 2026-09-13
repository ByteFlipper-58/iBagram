package org.telegram.messenger.feature.media.camera.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.camera.CameraController
import org.telegram.messenger.camera.CameraInfo
import org.telegram.messenger.core.data.BaseRemoteDataSource

/**
 * Remote/hardware data source coordinating device camera hardware initialization
 * and thread pool operations via CameraController.
 */
open class CameraRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    protected open fun isAndroidEnvironment(): Boolean {
        return try {
            ApplicationLoader.applicationContext != null
        } catch (_: Throwable) {
            false
        }
    }

    protected open fun getController(): CameraController? {
        return try {
            if (isAndroidEnvironment()) {
                CameraController.getInstance()
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    open fun isCameraInitialized(): Boolean {
        return try {
            getController()?.isCameraInitied ?: false
        } catch (_: Throwable) {
            false
        }
    }

    open fun getCameras(): List<CameraInfo>? {
        return try {
            getController()?.cameras
        } catch (_: Throwable) {
            null
        }
    }

    open fun initCamera(onInit: Runnable) {
        try {
            getController()?.initCamera(onInit)
        } catch (_: Throwable) {
            onInit.run()
        }
    }
}
