package org.telegram.messenger.feature.business.stargifts.domain.model

data class StarGiftFilter(
    val sortByDate: Boolean = true,
    val includeUnlimited: Boolean = true,
    val includeLimited: Boolean = true,
    val includeUpgradable: Boolean = true,
    val includeUnique: Boolean = true,
    val includeDisplayed: Boolean = true,
    val includeHidden: Boolean = true,
)
