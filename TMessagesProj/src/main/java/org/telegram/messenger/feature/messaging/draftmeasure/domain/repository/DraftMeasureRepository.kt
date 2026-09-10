package org.telegram.messenger.feature.messaging.draftmeasure.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureConfig
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureResult
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureTarget
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureViewport

/**
 * Repository interface for managing and calculating chat draft message height overrides.
 */
interface DraftMeasureRepository {
    fun getTarget(): DraftMeasureTarget
    fun setTarget(messageId: Int, groupId: Long): Boolean
    fun onMessageIdChanged(oldMessageId: Int, newMessageId: Int, groupId: Long): Boolean
    fun setPreviousMessageHeight(height: Int)
    fun getPreviousMessageHeight(): Int
    fun hasAdditionalHeight(): Boolean
    fun resetTarget(): Boolean
    fun calculateOverrideHeight(
        messageId: Int,
        groupId: Long,
        measuredHeight: Int,
        viewport: DraftMeasureViewport
    ): DraftMeasureResult
    fun getConfig(): DraftMeasureConfig
    fun observeConfig(): Flow<DraftMeasureConfig>
}
