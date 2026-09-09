package org.telegram.messenger.feature.biometrics.domain.model

data class BiometricKeyStateModel(
    val isKeyReady: Boolean = false,
    val hasChangedFingerprints: Boolean = false,
    val status: BiometricStatus = BiometricStatus.NotSupported
) {
    val canAuthenticate: Boolean
        get() = isKeyReady && !hasChangedFingerprints && status.isAvailable
}
