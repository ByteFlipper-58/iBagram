package org.telegram.messenger.feature.messaging.draftmeasure.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.draftmeasure.data.datasource.DraftMeasureLocalDataSource
import org.telegram.messenger.feature.messaging.draftmeasure.data.datasource.DraftMeasureRemoteDataSource
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureConfig
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureResult
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureTarget
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureViewport
import org.telegram.messenger.feature.messaging.draftmeasure.domain.repository.DraftMeasureRepository

class DraftMeasureRepositoryImpl(
    private val currentAccount: Int = 0,
    private val localDataSource: DraftMeasureLocalDataSource,
    private val remoteDataSource: DraftMeasureRemoteDataSource
) : DraftMeasureRepository {

    override fun getTarget(): DraftMeasureTarget = localDataSource.getTarget()

    override fun setTarget(messageId: Int, groupId: Long): Boolean =
        localDataSource.setTarget(messageId, groupId)

    override fun onMessageIdChanged(oldMessageId: Int, newMessageId: Int, groupId: Long): Boolean =
        localDataSource.onMessageIdChanged(oldMessageId, newMessageId, groupId)

    override fun setPreviousMessageHeight(height: Int) {
        localDataSource.setPreviousMessageHeight(height)
    }

    override fun getPreviousMessageHeight(): Int = localDataSource.getPreviousMessageHeight()

    override fun hasAdditionalHeight(): Boolean = localDataSource.hasAdditionalHeight()

    override fun resetTarget(): Boolean = localDataSource.resetTarget()

    override fun calculateOverrideHeight(
        messageId: Int,
        groupId: Long,
        measuredHeight: Int,
        viewport: DraftMeasureViewport
    ): DraftMeasureResult = localDataSource.calculateOverrideHeight(messageId, groupId, measuredHeight, viewport)

    override fun getConfig(): DraftMeasureConfig = localDataSource.getConfig()

    override fun observeConfig(): Flow<DraftMeasureConfig> = localDataSource.configFlow
}
