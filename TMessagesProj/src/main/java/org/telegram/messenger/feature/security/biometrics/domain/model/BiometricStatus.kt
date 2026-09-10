package org.telegram.messenger.feature.security.biometrics.domain.model

sealed class BiometricStatus {
    object Available : BiometricStatus()
    object HardwareUnavailable : BiometricStatus()
    object NoEnrolledBiometrics : BiometricStatus()
    object KeyPermanentlyInvalidated : BiometricStatus()
    object NotSupported : BiometricStatus()

    val isAvailable: Boolean
        get() = this is Available
}
