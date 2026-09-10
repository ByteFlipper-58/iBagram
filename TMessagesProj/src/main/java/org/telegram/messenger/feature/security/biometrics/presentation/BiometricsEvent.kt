package org.telegram.messenger.feature.security.biometrics.presentation

sealed class BiometricsEvent {
    data class CheckKeyReady(val notifyCheckFingerprint: Boolean = true) : BiometricsEvent()
    object DeleteInvalidKey : BiometricsEvent()
    object RefreshState : BiometricsEvent()
    object ClearError : BiometricsEvent()
}
