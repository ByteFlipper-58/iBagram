package org.telegram.messenger.feature.security.privacy.domain.model

data class TwoStepVerificationModel(
    val hasPassword: Boolean = false,
    val hasRecoveryEmail: Boolean = false,
    val emailPattern: String? = null
)
