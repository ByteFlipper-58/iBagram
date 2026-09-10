package org.telegram.messenger.feature.business.stargifts.domain.model

data class SavedStarGiftModel(
    val id: Long,
    val date: Int,
    val gift: StarGiftModel,
    val message: String? = null,
    val fromPeerId: Long? = null,
    val isPinnedToTop: Boolean = false,
    val isUnsaved: Boolean = false,
    val canUpgrade: Boolean = false,
    val convertStars: Long? = null,
    val upgradeStars: Long? = null,
    val transferStars: Long? = null,
)
