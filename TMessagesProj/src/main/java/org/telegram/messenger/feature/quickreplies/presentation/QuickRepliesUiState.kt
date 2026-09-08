package org.telegram.messenger.feature.quickreplies.presentation

import org.telegram.messenger.feature.quickreplies.domain.model.QuickReplyModel

data class QuickRepliesUiState(
    val replies: List<QuickReplyModel> = emptyList(),
    val canAddNew: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)
