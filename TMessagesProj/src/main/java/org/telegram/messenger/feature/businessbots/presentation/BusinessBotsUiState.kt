package org.telegram.messenger.feature.businessbots.presentation

import org.telegram.messenger.feature.businessbots.domain.model.ConnectedBotModel

data class BusinessBotsUiState(
    val connectedBots: List<ConnectedBotModel> = emptyList(),
    val selectedBot: ConnectedBotModel? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
) {
    val hasConnectedBot: Boolean
        get() = connectedBots.isNotEmpty()

    val currentBot: ConnectedBotModel?
        get() = selectedBot ?: connectedBots.firstOrNull()
}
