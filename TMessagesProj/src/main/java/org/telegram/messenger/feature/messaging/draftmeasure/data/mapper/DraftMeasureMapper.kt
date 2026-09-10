package org.telegram.messenger.feature.messaging.draftmeasure.data.mapper

import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureConfig
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureResult
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureTarget
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureViewport

object DraftMeasureMapper {
    fun toTarget(messageId: Int, groupId: Long): DraftMeasureTarget {
        return DraftMeasureTarget(
            messageId = messageId,
            groupId = groupId
        )
    }

    fun toResult(
        measuredHeight: Int,
        additionalHeight: Int,
        hasAdditionalHeight: Boolean
    ): DraftMeasureResult {
        return DraftMeasureResult(
            measuredHeight = measuredHeight,
            additionalHeight = additionalHeight,
            finalHeight = measuredHeight + additionalHeight,
            hasAdditionalHeight = hasAdditionalHeight
        )
    }

    fun toConfig(
        target: DraftMeasureTarget,
        previousMessageHeight: Int,
        hasAdditionalHeight: Boolean
    ): DraftMeasureConfig {
        return DraftMeasureConfig(
            target = target,
            previousMessageHeight = previousMessageHeight,
            hasAdditionalHeight = hasAdditionalHeight
        )
    }
}
