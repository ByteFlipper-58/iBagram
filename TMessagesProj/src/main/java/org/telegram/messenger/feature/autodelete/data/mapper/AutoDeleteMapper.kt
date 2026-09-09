package org.telegram.messenger.feature.autodelete.data.mapper

import org.telegram.messenger.feature.autodelete.domain.model.AutoDeleteTtlModel
import org.telegram.messenger.feature.autodelete.domain.model.ChatAutoDeleteStateModel
import org.telegram.messenger.feature.autodelete.domain.model.GlobalAutoDeleteStateModel

object AutoDeleteMapper {

    fun toModel(periodSeconds: Int): AutoDeleteTtlModel {
        return AutoDeleteTtlModel(periodSeconds.coerceAtLeast(0))
    }

    fun toModelFromMinutes(periodMinutes: Int): AutoDeleteTtlModel {
        return AutoDeleteTtlModel.fromMinutes(periodMinutes.coerceAtLeast(0))
    }

    fun toGlobalState(periodMinutes: Int, isLoading: Boolean = false): GlobalAutoDeleteStateModel {
        return GlobalAutoDeleteStateModel(
            ttl = toModelFromMinutes(periodMinutes),
            isLoading = isLoading
        )
    }

    fun toChatState(chatId: Long, periodSeconds: Int): ChatAutoDeleteStateModel {
        return ChatAutoDeleteStateModel(
            chatId = chatId,
            ttl = toModel(periodSeconds)
        )
    }
}
