package org.telegram.messenger.feature.messaging.factcheck.presentation

import org.telegram.messenger.feature.messaging.factcheck.domain.model.FactCheckEntityModel

sealed class FactCheckEvent {
    data class LoadFactCheck(
        val dialogId: Long,
        val messageId: Int,
        val hash: Long = 0L
    ) : FactCheckEvent()

    data class UpdateInputText(val text: String) : FactCheckEvent()

    data class SetEntities(val entities: List<FactCheckEntityModel>) : FactCheckEvent()

    object ApplyFactCheck : FactCheckEvent()

    object DeleteFactCheck : FactCheckEvent()

    object ClearMessages : FactCheckEvent()
}
