package org.telegram.messenger.feature.social.boosts.presentation

import org.telegram.messenger.feature.social.boosts.domain.model.BoostStatusModel
import org.telegram.messenger.feature.social.boosts.domain.model.CanApplyBoostModel
import org.telegram.messenger.feature.social.boosts.domain.model.MyBoostsModel

data class BoostsUiState(
    val status: BoostStatusModel? = null,
    val myBoosts: MyBoostsModel? = null,
    val canApply: CanApplyBoostModel? = null,
    val isLoading: Boolean = false,
    val isApplying: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)
