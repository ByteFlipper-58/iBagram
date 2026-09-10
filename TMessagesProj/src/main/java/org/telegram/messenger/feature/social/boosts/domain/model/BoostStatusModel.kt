package org.telegram.messenger.feature.social.boosts.domain.model

data class BoostStatusModel(
    val level: Int,
    val currentLevelBoosts: Int,
    val boosts: Int,
    val giftBoosts: Int,
    val nextLevelBoosts: Int,
    val premiumAudiencePart: Double,
    val premiumAudienceTotal: Double,
    val boostUrl: String,
    val hasMyBoost: Boolean,
    val myBoostSlots: List<Int>,
    val isMaxLevel: Boolean
)
