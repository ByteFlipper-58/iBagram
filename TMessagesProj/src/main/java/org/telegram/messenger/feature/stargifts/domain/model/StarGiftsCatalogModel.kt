package org.telegram.messenger.feature.stargifts.domain.model

data class StarGiftsCatalogModel(
    val gifts: List<StarGiftModel> = emptyList(),
    val isLoading: Boolean = false,
)
