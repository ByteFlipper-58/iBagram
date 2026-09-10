package org.telegram.messenger.feature.social.boosts.domain.model

data class BoostSlotModel(
    val slot: Int,
    val peerDialogId: Long?,
    val date: Int,
    val expires: Int,
    val cooldownUntilDate: Int
)
