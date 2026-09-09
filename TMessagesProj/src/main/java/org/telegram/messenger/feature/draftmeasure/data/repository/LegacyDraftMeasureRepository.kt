package org.telegram.messenger.feature.draftmeasure.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.draftmeasure.data.mapper.DraftMeasureMapper
import org.telegram.messenger.feature.draftmeasure.domain.model.DraftMeasureConfig
import org.telegram.messenger.feature.draftmeasure.domain.model.DraftMeasureResult
import org.telegram.messenger.feature.draftmeasure.domain.model.DraftMeasureTarget
import org.telegram.messenger.feature.draftmeasure.domain.model.DraftMeasureViewport
import org.telegram.messenger.feature.draftmeasure.domain.repository.DraftMeasureRepository
import org.telegram.ui.Components.chat.ChatActivityDraftMessageMeasureController

class LegacyDraftMeasureRepository(
    private val legacyController: ChatActivityDraftMessageMeasureController? = null
) : DraftMeasureRepository {

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

    override fun getTarget(): DraftMeasureTarget {
        synchronized(stateLock) {
            return currentTarget
        }
    }

    override fun setTarget(messageId: Int, groupId: Long): Boolean {
        synchronized(stateLock) {
            val changed = currentTarget.messageId != messageId || currentTarget.groupId != groupId
            if (changed) {
                currentTarget = DraftMeasureMapper.toTarget(messageId, groupId)
                if (messageId == 0) {
                    currentHasAdditionalHeight = false
                }
                legacyController?.setMessageIdToOverride(messageId, groupId)
                emitConfigLocked()
            }
            return changed
        }
    }

    override fun onMessageIdChanged(oldMessageId: Int, newMessageId: Int, groupId: Long): Boolean {
        synchronized(stateLock) {
            if (currentTarget.messageId == oldMessageId) {
                legacyController?.onMessageIdChanged(oldMessageId, newMessageId, groupId)
                return setTarget(newMessageId, groupId)
            }
            return false
        }
    }

    override fun setPreviousMessageHeight(height: Int) {
        synchronized(stateLock) {
            currentPreviousHeight = height
            legacyController?.setPreviousMessageHeight(height)
            emitConfigLocked()
        }
    }

    override fun getPreviousMessageHeight(): Int {
        synchronized(stateLock) {
            return currentPreviousHeight
        }
    }

    override fun hasAdditionalHeight(): Boolean {
        synchronized(stateLock) {
            return legacyController?.hasAdditionalHeight() ?: currentHasAdditionalHeight
        }
    }

    override fun resetTarget(): Boolean {
        return setTarget(0, 0L)
    }

    override fun calculateOverrideHeight(
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
                legacyController?.setMessageIdToOverride(0, 0L)
            }

            emitConfigLocked()

            return DraftMeasureMapper.toResult(
                measuredHeight = measuredHeight,
                additionalHeight = additionalHeight,
                hasAdditionalHeight = currentHasAdditionalHeight
            )
        }
    }

    override fun getConfig(): DraftMeasureConfig {
        synchronized(stateLock) {
            return DraftMeasureMapper.toConfig(
                target = currentTarget,
                previousMessageHeight = currentPreviousHeight,
                hasAdditionalHeight = currentHasAdditionalHeight
            )
        }
    }

    override fun observeConfig(): Flow<DraftMeasureConfig> {
        return _configFlow.asStateFlow()
    }

    private fun emitConfigLocked() {
        _configFlow.value = DraftMeasureMapper.toConfig(
            target = currentTarget,
            previousMessageHeight = currentPreviousHeight,
            hasAdditionalHeight = currentHasAdditionalHeight
        )
    }
}
