package org.telegram.messenger.feature.messaging.draftmeasure.presentation

import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureViewport

sealed class DraftMeasureEvent {
    data class SetTarget(val messageId: Int, val groupId: Long = 0L) : DraftMeasureEvent()
    data class MessageIdChanged(val oldMessageId: Int, val newMessageId: Int, val groupId: Long = 0L) : DraftMeasureEvent()
    data class SetPreviousHeight(val height: Int) : DraftMeasureEvent()
    object ResetTarget : DraftMeasureEvent()
    data class CalculateOverride(
        val messageId: Int,
        val groupId: Long,
        val measuredHeight: Int,
        val viewport: DraftMeasureViewport
    ) : DraftMeasureEvent()
}
