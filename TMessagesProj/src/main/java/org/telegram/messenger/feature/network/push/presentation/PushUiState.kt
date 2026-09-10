package org.telegram.messenger.feature.network.push.presentation

import org.telegram.messenger.feature.network.push.domain.model.PushStatusModel

data class PushUiState(
    val isLoading: Boolean = false,
    val status: PushStatusModel? = null,
    val isAvailable: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)
