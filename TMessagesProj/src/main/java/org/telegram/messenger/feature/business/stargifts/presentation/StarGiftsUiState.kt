package org.telegram.messenger.feature.business.stargifts.presentation

import org.telegram.messenger.feature.business.stargifts.domain.model.ProfileGiftsModel
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftFilter
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftModel

sealed interface StarGiftsUiState {
    object Initial : StarGiftsUiState
    object Loading : StarGiftsUiState
    data class Success(
        val catalog: List<StarGiftModel> = emptyList(),
        val profileGifts: ProfileGiftsModel? = null,
        val filter: StarGiftFilter = StarGiftFilter(),
        val selectedGift: StarGiftModel? = null,
        val isProcessing: Boolean = false,
        val errorMessage: String? = null
    ) : StarGiftsUiState
    data class Error(val message: String) : StarGiftsUiState
}
