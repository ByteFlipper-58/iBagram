package org.telegram.messenger.feature.stargifts.domain.model

data class ProfileGiftsModel(
    val dialogId: Long,
    val gifts: List<SavedStarGiftModel> = emptyList(),
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val nextOffset: String? = null,
    val isLoading: Boolean = false,
)
