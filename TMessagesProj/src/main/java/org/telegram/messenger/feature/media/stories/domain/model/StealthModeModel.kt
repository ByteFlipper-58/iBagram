package org.telegram.messenger.feature.media.stories.domain.model

data class StealthModeModel(
    val activeUntilDate: Long = 0L,
    val cooldownUntilDate: Long = 0L,
    val isActive: Boolean = false,
    val canActivateFuture: Boolean = true
)
