package org.telegram.messenger.feature.bottomviews.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.bottomviews.data.mapper.BottomViewsVisibilityMapper
import org.telegram.messenger.feature.bottomviews.domain.model.BottomViewsVisibilityState
import org.telegram.messenger.feature.bottomviews.domain.repository.BottomViewsVisibilityRepository
import org.telegram.ui.Components.chat.ChatActivityBottomViewsVisibilityController

class LegacyBottomViewsVisibilityRepository(
    private val legacyController: ChatActivityBottomViewsVisibilityController? = null
) : BottomViewsVisibilityRepository {

    private val lock = Any()
    private var flags: Int = 1
    private val inMemoryVisibilities = FloatArray(32)

    private val _stateFlow = MutableStateFlow(
        BottomViewsVisibilityState(
            visibilityFlags = 1,
            priorityContainerId = 0,
            visibilities = mapOf(0 to 1.0f)
        )
    )

    init {
        inMemoryVisibilities[0] = 1.0f
    }

    override fun getVisibility(containerId: Int): Float {
        synchronized(lock) {
            if (legacyController != null) {
                return legacyController.getVisibility(containerId)
            }
            if (containerId !in 0..31) return 0.0f
            return inMemoryVisibilities[containerId]
        }
    }

    override fun setViewVisible(containerId: Int, isVisible: Boolean, animated: Boolean) {
        synchronized(lock) {
            if (containerId !in 0..31) return

            if (legacyController != null) {
                legacyController.setViewVisible(containerId, isVisible, animated)
                flags = if (isVisible) flags or (1 shl containerId) else flags and (1 shl containerId).inv()
                emitStateLocked()
                return
            }

            val oldPriority = BottomViewsVisibilityMapper.calculatePriorityContainerId(flags)
            flags = if (isVisible) {
                flags or (1 shl containerId)
            } else {
                flags and (1 shl containerId).inv()
            }
            val newPriority = BottomViewsVisibilityMapper.calculatePriorityContainerId(flags)

            for (i in 0 until 32) {
                inMemoryVisibilities[i] = if (i == newPriority && (flags and (1 shl i)) != 0) 1.0f else 0.0f
            }

            emitStateLocked()
        }
    }

    override fun getCurrentPriorityContainerId(): Int {
        synchronized(lock) {
            if (legacyController != null) {
                return legacyController.getCurrentPriorityContainerId()
            }
            return BottomViewsVisibilityMapper.calculatePriorityContainerId(flags)
        }
    }

    override fun getState(): BottomViewsVisibilityState {
        synchronized(lock) {
            return _stateFlow.value
        }
    }

    override fun observeState(): Flow<BottomViewsVisibilityState> {
        return _stateFlow.asStateFlow()
    }

    private fun emitStateLocked() {
        val priorityId = if (legacyController != null) {
            legacyController.getCurrentPriorityContainerId()
        } else {
            BottomViewsVisibilityMapper.calculatePriorityContainerId(flags)
        }

        val map = HashMap<Int, Float>(32)
        for (i in 0 until 32) {
            val v = if (legacyController != null) legacyController.getVisibility(i) else inMemoryVisibilities[i]
            if (v > 0f) {
                map[i] = v
            }
        }
        if (!map.containsKey(priorityId) && (flags and (1 shl priorityId)) != 0) {
            map[priorityId] = 1.0f
        }

        _stateFlow.value = BottomViewsVisibilityMapper.toState(
            flags = flags,
            priorityId = priorityId,
            visibilities = map
        )
    }
}
