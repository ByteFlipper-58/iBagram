package org.telegram.messenger.feature.privacy.domain.model

data class TwoStepVerificationModel(
    val hasPassword: Boolean = false,
    val hasRecoveryEmail: Boolean = false,
    val emailPattern: String? = null
)
