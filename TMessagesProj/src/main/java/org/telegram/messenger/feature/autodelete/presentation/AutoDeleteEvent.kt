package org.telegram.messenger.feature.autodelete.presentation

import org.telegram.messenger.feature.autodelete.domain.model.AutoDeleteTtlModel

sealed interface AutoDeleteEvent {
    data class LoadGlobalTtl(val forceRefresh: Boolean = false) : AutoDeleteEvent
    data class SetGlobalTtl(val ttl: AutoDeleteTtlModel) : AutoDeleteEvent
    data class SetChatTtl(val chatId: Long, val ttl: AutoDeleteTtlModel) : AutoDeleteEvent
    data class SetChatsTtlBatch(val chatIds: List<Long>, val ttl: AutoDeleteTtlModel) : AutoDeleteEvent
    data object ClearError : AutoDeleteEvent
}
