package org.telegram.messenger.feature.security.biometrics.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.biometrics.data.datasource.BiometricsLocalDataSource
import org.telegram.messenger.feature.security.biometrics.data.mapper.BiometricMapper
import org.telegram.messenger.feature.security.biometrics.domain.model.BiometricKeyStateModel
import org.telegram.messenger.feature.security.biometrics.domain.repository.BiometricsRepository

/**
 * Production implementation of [BiometricsRepository] delegating hardware and Keystore
 * operations to [BiometricsLocalDataSource] and listening to NotificationCenter key generation events.
 */
class BiometricsRepositoryImpl(
    private val localDataSource: BiometricsLocalDataSource = BiometricsLocalDataSource(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BiometricsRepository {

    override fun observeKeyState(): Flow<BiometricKeyStateModel> {
        return NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.didGenerateFingerprintKeyPair)
            .map { getKeyState() }
            .onStart { emit(getKeyState()) }
            .flowOn(ioDispatcher)
    }

    override suspend fun getKeyState(): BiometricKeyStateModel = withContext(ioDispatcher) {
        val supported = localDataSource.isSupported()
        if (!supported) {
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

        val isHardwareDetected = localDataSource.isHardwareDetected()
        val hasEnrolledBiometrics = localDataSource.hasEnrolledBiometrics()
        val isKeyReady = localDataSource.isKeyReady()
        val hasChangedFingerprints = localDataSource.hasDeviceBiometricsChanged()

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
            if (localDataSource.isSupported()) {
                localDataSource.checkKeyReady(notifyCheckFingerprint)
                Result.Success(localDataSource.isKeyReady())
            } else {
                Result.Success(false)
            }
        } catch (e: Throwable) {
            Result.failure(e.message ?: "checkKeyReady failed", e)
        }
    }

    override suspend fun deleteInvalidKey(): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (localDataSource.isSupported()) {
                localDataSource.deleteInvalidKey()
            }
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.failure(e.message ?: "deleteInvalidKey failed", e)
        }
    }

    override fun isKeyReady(): Boolean {
        return localDataSource.isKeyReady()
    }

    override fun hasDeviceBiometricsChanged(): Boolean {
        return localDataSource.hasDeviceBiometricsChanged()
    }
}
