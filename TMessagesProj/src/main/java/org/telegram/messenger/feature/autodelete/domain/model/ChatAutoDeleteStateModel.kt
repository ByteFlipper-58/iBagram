package org.telegram.messenger.feature.autodelete.domain.model

data class ChatAutoDeleteStateModel(
    val chatId: Long,
    val ttl: AutoDeleteTtlModel = AutoDeleteTtlModel.OFF
)
