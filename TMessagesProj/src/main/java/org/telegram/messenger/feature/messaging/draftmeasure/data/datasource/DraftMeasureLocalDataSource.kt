package org.telegram.messenger.feature.messaging.draftmeasure.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.messaging.draftmeasure.data.mapper.DraftMeasureMapper
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureConfig
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureResult
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureTarget
import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureViewport

class DraftMeasureLocalDataSource(
    private val currentAccount: Int = 0
) {
    private val stateLock = Any()
    private var currentTarget: DraftMeasureTarget = DraftMeasureTarget.EMPTY
    private var currentPreviousHeight: Int = 0
    private var currentHasAdditionalHeight: Boolean = false

    private val _configFlow = MutableStateFlow(
        DraftMeasureConfig(
            target = currentTarget,
            previousMessageHeight = currentPreviousHeight,
            hasAdditionalHeight = currentHasAdditionalHeight
        )
    )
    val configFlow: Flow<DraftMeasureConfig> = _configFlow.asStateFlow()

    fun getTarget(): DraftMeasureTarget {
        synchronized(stateLock) {
            return currentTarget
        }
    }

    fun setTarget(messageId: Int, groupId: Long): Boolean {
        synchronized(stateLock) {
            val changed = currentTarget.messageId != messageId || currentTarget.groupId != groupId
            if (changed) {
                currentTarget = DraftMeasureMapper.toTarget(messageId, groupId)
                if (messageId == 0) {
                    currentHasAdditionalHeight = false
                }
                emitConfigLocked()
            }
            return changed
        }
    }

    fun onMessageIdChanged(oldMessageId: Int, newMessageId: Int, groupId: Long): Boolean {
        synchronized(stateLock) {
            if (currentTarget.messageId == oldMessageId) {
                return setTarget(newMessageId, groupId)
            }
            return false
        }
    }

    fun setPreviousMessageHeight(height: Int) {
        synchronized(stateLock) {
            currentPreviousHeight = height
            emitConfigLocked()
        }
    }

    fun getPreviousMessageHeight(): Int {
        synchronized(stateLock) {
            return currentPreviousHeight
        }
    }

    fun hasAdditionalHeight(): Boolean {
        synchronized(stateLock) {
            return currentHasAdditionalHeight
        }
    }

    fun resetTarget(): Boolean {
        return setTarget(0, 0L)
    }

    fun calculateOverrideHeight(
        messageId: Int,
        groupId: Long,
        measuredHeight: Int,
        viewport: DraftMeasureViewport
    ): DraftMeasureResult {
        synchronized(stateLock) {
            if (!currentTarget.matches(messageId, groupId)) {
                return DraftMeasureMapper.toResult(
                    measuredHeight = measuredHeight,
                    additionalHeight = 0,
                    hasAdditionalHeight = currentHasAdditionalHeight
                )
            }

            val availHeight = (viewport.totalHeight - viewport.paddingTop - viewport.paddingBottom - currentPreviousHeight)
                .coerceAtLeast(0)

            val additionalHeight = (availHeight - measuredHeight).coerceAtLeast(0)
            currentHasAdditionalHeight = additionalHeight > 0

            if (currentTarget.messageId > 0 && !currentHasAdditionalHeight) {
                currentTarget = DraftMeasureTarget.EMPTY
            }

            emitConfigLocked()

            return DraftMeasureMapper.toResult(
                measuredHeight = measuredHeight,
                additionalHeight = additionalHeight,
                hasAdditionalHeight = currentHasAdditionalHeight
            )
        }
    }

    fun getConfig(): DraftMeasureConfig {
        synchronized(stateLock) {
            return DraftMeasureMapper.toConfig(
                target = currentTarget,
                previousMessageHeight = currentPreviousHeight,
                hasAdditionalHeight = currentHasAdditionalHeight
            )
        }
    }

    private fun emitConfigLocked() {
        _configFlow.value = DraftMeasureMapper.toConfig(
            target = currentTarget,
            previousMessageHeight = currentPreviousHeight,
            hasAdditionalHeight = currentHasAdditionalHeight
        )
    }
}
