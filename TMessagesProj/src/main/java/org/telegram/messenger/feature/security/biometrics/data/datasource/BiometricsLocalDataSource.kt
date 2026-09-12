package org.telegram.messenger.feature.security.biometrics.data.datasource

import android.os.Build
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FingerprintController
import org.telegram.messenger.support.fingerprint.FingerprintManagerCompat

/**
 * Local data source managing device biometrics hardware checks, Keystore operations,
 * and key invalidation state via [FingerprintController].
 */
open class BiometricsLocalDataSource {

    /**
     * Checks if biometrics/fingerprint features are supported by the Android OS version.
     */
    open fun isSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    /**
     * Checks if biometric hardware is detected on this device.
     */
    open fun isHardwareDetected(): Boolean {
        val appContext = try {
            ApplicationLoader.applicationContext
        } catch (_: Throwable) {
            null
        }
        return try {
            appContext != null && FingerprintManagerCompat.from(appContext).isHardwareDetected
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Checks if the user has enrolled biometrics/fingerprints on the device.
     */
    open fun hasEnrolledBiometrics(): Boolean {
        val appContext = try {
            ApplicationLoader.applicationContext
        } catch (_: Throwable) {
            null
        }
        return try {
            appContext != null && FingerprintManagerCompat.from(appContext).hasEnrolledFingerprints()
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Checks if the biometric cryptographic key is ready in the Android KeyStore.
     */
    open fun isKeyReady(): Boolean {
        return if (isSupported()) {
            try {
                FingerprintController.isKeyReady()
            } catch (_: Throwable) {
                false
            }
        } else {
            false
        }
    }

    /**
     * Checks if device biometrics have changed, permanently invalidating the current key.
     */
    open fun hasDeviceBiometricsChanged(): Boolean {
        return if (isSupported()) {
            try {
                FingerprintController.checkDeviceFingerprintsChanged()
            } catch (_: Throwable) {
                false
            }
        } else {
            false
        }
    }

    /**
     * Triggers asynchronous key verification and generation if necessary.
     */
    open fun checkKeyReady(notifyCheckFingerprint: Boolean) {
        if (isSupported()) {
            FingerprintController.checkKeyReady(notifyCheckFingerprint)
        }
    }

    /**
     * Deletes invalidated biometric key from the Android KeyStore.
     */
    open fun deleteInvalidKey() {
        if (isSupported()) {
            FingerprintController.deleteInvalidKey()
        }
    }
}
