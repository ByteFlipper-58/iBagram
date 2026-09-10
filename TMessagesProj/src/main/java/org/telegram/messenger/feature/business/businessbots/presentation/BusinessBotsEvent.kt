package org.telegram.messenger.feature.business.businessbots.presentation

import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRecipientsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRightsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.ConnectedBotModel

sealed interface BusinessBotsEvent {
    data class Load(val forceReload: Boolean = false) : BusinessBotsEvent
    data class SelectBot(val bot: ConnectedBotModel?) : BusinessBotsEvent
    data class UpdateBot(
        val botId: Long,
        val rights: BusinessBotRightsModel,
        val recipients: BusinessBotRecipientsModel
    ) : BusinessBotsEvent
    data class DeleteBot(val botId: Long) : BusinessBotsEvent
    object ClearMessages : BusinessBotsEvent
}
