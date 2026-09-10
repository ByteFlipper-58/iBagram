package org.telegram.messenger.feature.security.biometrics.data.repository

import android.os.Build
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FingerprintController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.biometrics.data.mapper.BiometricMapper
import org.telegram.messenger.feature.security.biometrics.domain.model.BiometricKeyStateModel
import org.telegram.messenger.feature.security.biometrics.domain.repository.BiometricsRepository
import org.telegram.messenger.support.fingerprint.FingerprintManagerCompat

class LegacyBiometricsRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BiometricsRepository {

    override fun observeKeyState(): Flow<BiometricKeyStateModel> {
        return NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.didGenerateFingerprintKeyPair)
            .map { getKeyState() }
            .onStart { emit(getKeyState()) }
            .flowOn(ioDispatcher)
    }

    override suspend fun getKeyState(): BiometricKeyStateModel = withContext(ioDispatcher) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return@withContext BiometricMapper.toKeyStateModel(
                isKeyReady = false,
                hasChangedFingerprints = false,
                status = BiometricMapper.mapStatus(
                    isSupported = false,
                    isHardwareDetected = false,
                    hasEnrolledBiometrics = false,
                    isPermanentlyInvalidated = false
                )
            )
        }

        val appContext = try {
            ApplicationLoader.applicationContext
        } catch (_: Throwable) {
            null
        }

        val isHardwareDetected = try {
            appContext != null && FingerprintManagerCompat.from(appContext).isHardwareDetected
        } catch (_: Throwable) {
            false
        }

        val hasEnrolledBiometrics = try {
            appContext != null && FingerprintManagerCompat.from(appContext).hasEnrolledFingerprints()
        } catch (_: Throwable) {
            false
        }

        val isKeyReady = try {
            FingerprintController.isKeyReady()
        } catch (_: Throwable) {
            false
        }

        val hasChangedFingerprints = try {
            FingerprintController.checkDeviceFingerprintsChanged()
        } catch (_: Throwable) {
            false
        }

        val status = BiometricMapper.mapStatus(
            isSupported = true,
            isHardwareDetected = isHardwareDetected,
            hasEnrolledBiometrics = hasEnrolledBiometrics,
            isPermanentlyInvalidated = hasChangedFingerprints
        )

        BiometricMapper.toKeyStateModel(
            isKeyReady = isKeyReady,
            hasChangedFingerprints = hasChangedFingerprints,
            status = status
        )
    }

    override suspend fun checkKeyReady(notifyCheckFingerprint: Boolean): Result<Boolean> = withContext(ioDispatcher) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                FingerprintController.checkKeyReady(notifyCheckFingerprint)
                Result.Success(FingerprintController.isKeyReady())
            } else {
                Result.Success(false)
            }
        } catch (e: Throwable) {
            Result.failure(e.message ?: "checkKeyReady failed", e)
        }
    }

    override suspend fun deleteInvalidKey(): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                FingerprintController.deleteInvalidKey()
            }
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.failure(e.message ?: "deleteInvalidKey failed", e)
        }
    }

    override fun isKeyReady(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                FingerprintController.isKeyReady()
            } catch (_: Throwable) {
                false
            }
        } else {
            false
        }
    }

    override fun hasDeviceBiometricsChanged(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                FingerprintController.checkDeviceFingerprintsChanged()
            } catch (_: Throwable) {
                false
            }
        } else {
            false
        }
    }
}
