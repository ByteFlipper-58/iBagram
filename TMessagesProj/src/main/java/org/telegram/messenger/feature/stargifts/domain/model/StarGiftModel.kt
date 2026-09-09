package org.telegram.messenger.feature.stargifts.domain.model

data class StarGiftModel(
    val id: Long,
    val stars: Long,
    val slug: String? = null,
    val title: String? = null,
    val stickerDocumentId: Long? = null,
    val availabilityRemains: Int? = null,
    val availabilityTotal: Int? = null,
    val isSoldOut: Boolean = false,
    val isBirthday: Boolean = false,
    val isLimited: Boolean = false,
    val upgradeStars: Long? = null,
    val transferStars: Long? = null,
    val canExportAt: Int? = null,
)
