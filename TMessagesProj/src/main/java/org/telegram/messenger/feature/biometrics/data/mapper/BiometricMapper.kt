package org.telegram.messenger.feature.biometrics.data.mapper

import org.telegram.messenger.feature.biometrics.domain.model.BiometricKeyStateModel
import org.telegram.messenger.feature.biometrics.domain.model.BiometricStatus

object BiometricMapper {

    fun mapStatus(
        isSupported: Boolean,
        isHardwareDetected: Boolean,
        hasEnrolledBiometrics: Boolean,
        isPermanentlyInvalidated: Boolean
    ): BiometricStatus {
        return when {
            !isSupported -> BiometricStatus.NotSupported
            !isHardwareDetected -> BiometricStatus.HardwareUnavailable
            !hasEnrolledBiometrics -> BiometricStatus.NoEnrolledBiometrics
            isPermanentlyInvalidated -> BiometricStatus.KeyPermanentlyInvalidated
            else -> BiometricStatus.Available
        }
    }

    fun toKeyStateModel(
        isKeyReady: Boolean,
        hasChangedFingerprints: Boolean,
        status: BiometricStatus
    ): BiometricKeyStateModel {
        return BiometricKeyStateModel(
            isKeyReady = isKeyReady,
            hasChangedFingerprints = hasChangedFingerprints,
            status = status
        )
    }
}
