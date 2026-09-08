package org.telegram.messenger.feature.reactions.domain.model

data class ReactionsSettingsModel(
    val doubleTapReaction: String? = null,
    val availableReactions: List<ReactionItemModel> = emptyList(),
    val recentReactions: List<ReactionItemModel> = emptyList(),
    val topReactions: List<ReactionItemModel> = emptyList()
)
