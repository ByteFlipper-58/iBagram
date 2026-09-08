package org.telegram.messenger.feature.boosts.presentation

import org.telegram.messenger.feature.boosts.domain.model.BoostStatusModel
import org.telegram.messenger.feature.boosts.domain.model.CanApplyBoostModel
import org.telegram.messenger.feature.boosts.domain.model.MyBoostsModel

data class BoostsUiState(
    val status: BoostStatusModel? = null,
    val myBoosts: MyBoostsModel? = null,
    val canApply: CanApplyBoostModel? = null,
    val isLoading: Boolean = false,
    val isApplying: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)
