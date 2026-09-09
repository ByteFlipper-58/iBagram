package org.telegram.messenger.feature.businessbots.domain.model

data class BusinessBotsStateModel(
    val connectedBots: List<ConnectedBotModel> = emptyList(),
    val isLoading: Boolean = false
) {
    val firstBot: ConnectedBotModel?
        get() = connectedBots.firstOrNull()

    val hasConnectedBot: Boolean
        get() = connectedBots.isNotEmpty()
}
