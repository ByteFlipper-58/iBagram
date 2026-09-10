package org.telegram.messenger.feature.social.joinrequests.presentation

import org.telegram.messenger.feature.social.joinrequests.domain.model.JoinRequestModel

data class JoinRequestsUiState(
    val chatId: Long = 0L,
    val pendingCount: Int = 0,
    val requests: List<JoinRequestModel> = emptyList(),
    val searchQuery: String = "",
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)
