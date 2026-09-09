package org.telegram.messenger.feature.businessbots.domain.model

data class ConnectedBotModel(
    val botId: Long,
    val recipients: BusinessBotRecipientsModel = BusinessBotRecipientsModel(),
    val rights: BusinessBotRightsModel = BusinessBotRightsModel.makeDefault(),
    val device: String? = null,
    val location: String? = null,
    val date: Int = 0
)
