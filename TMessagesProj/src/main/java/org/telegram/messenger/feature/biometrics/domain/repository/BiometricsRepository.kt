package org.telegram.messenger.feature.biometrics.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.biometrics.domain.model.BiometricKeyStateModel

interface BiometricsRepository {
    fun observeKeyState(): Flow<BiometricKeyStateModel>
    suspend fun getKeyState(): BiometricKeyStateModel
    suspend fun checkKeyReady(notifyCheckFingerprint: Boolean = true): Result<Boolean>
    suspend fun deleteInvalidKey(): Result<Unit>
    fun isKeyReady(): Boolean
    fun hasDeviceBiometricsChanged(): Boolean
}
