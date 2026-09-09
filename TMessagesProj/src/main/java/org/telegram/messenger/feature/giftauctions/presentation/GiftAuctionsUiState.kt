package org.telegram.messenger.feature.giftauctions.presentation

import org.telegram.messenger.feature.giftauctions.domain.model.GiftAuctionAcquiredGiftModel
import org.telegram.messenger.feature.giftauctions.domain.model.GiftAuctionModel

sealed class GiftAuctionsUiState {
    object Initial : GiftAuctionsUiState()
    object Loading : GiftAuctionsUiState()

    data class Success(
        val activeAuctions: List<GiftAuctionModel> = emptyList(),
        val selectedAuction: GiftAuctionModel? = null,
        val acquiredGifts: List<GiftAuctionAcquiredGiftModel> = emptyList(),
        val isBidding: Boolean = false,
        val isRefreshing: Boolean = false,
        val errorMessage: String? = null
    ) : GiftAuctionsUiState() {
        val hasActiveAuctions: Boolean
            get() = activeAuctions.isNotEmpty()
    }

    data class Error(val message: String?) : GiftAuctionsUiState()
}
