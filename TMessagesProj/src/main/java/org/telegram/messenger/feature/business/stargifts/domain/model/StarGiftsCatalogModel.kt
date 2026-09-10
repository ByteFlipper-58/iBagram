package org.telegram.messenger.feature.business.stargifts.domain.model

data class StarGiftsCatalogModel(
    val gifts: List<StarGiftModel> = emptyList(),
    val isLoading: Boolean = false,
)
