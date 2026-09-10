package org.telegram.messenger.feature.messaging.autodelete.domain.model

data class ChatAutoDeleteStateModel(
    val chatId: Long,
    val ttl: AutoDeleteTtlModel = AutoDeleteTtlModel.OFF
)
