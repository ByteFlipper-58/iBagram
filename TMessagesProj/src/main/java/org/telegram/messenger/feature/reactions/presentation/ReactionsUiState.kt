package org.telegram.messenger.feature.reactions.presentation

import org.telegram.messenger.feature.reactions.domain.model.ReactionItemModel
import org.telegram.messenger.feature.reactions.domain.model.ReactionsSettingsModel

data class ReactionsUiState(
    val settings: ReactionsSettingsModel = ReactionsSettingsModel(),
    val availableReactions: List<ReactionItemModel> = emptyList(),
    val recentReactions: List<ReactionItemModel> = emptyList(),
    val doubleTapReaction: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)
