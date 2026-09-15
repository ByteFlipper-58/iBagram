package org.telegram.messenger.feature.messaging.bottomviews.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.messaging.bottomviews.data.mapper.BottomViewsVisibilityMapper
import org.telegram.messenger.feature.messaging.bottomviews.domain.model.BottomViewsVisibilityState

class BottomViewsLocalDataSource(
    private val currentAccount: Int = 0
) {
    private val lock = Any()
    private var flags: Int = 1
    private val inMemoryVisibilities = FloatArray(32)

    private val _state = MutableStateFlow(
        BottomViewsVisibilityState(
            visibilityFlags = 1,
            priorityContainerId = 0,
            visibilities = mapOf(0 to 1.0f)
        )
    )
    val state: StateFlow<BottomViewsVisibilityState> = _state.asStateFlow()

    init {
        inMemoryVisibilities[0] = 1.0f
    }

    fun getVisibility(containerId: Int): Float = synchronized(lock) {
        if (containerId !in 0..31) return 0.0f
        return inMemoryVisibilities[containerId]
    }

    fun setViewVisible(containerId: Int, isVisible: Boolean, animated: Boolean = true) = synchronized(lock) {
        if (containerId !in 0..31) return

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

    fun getCurrentPriorityContainerId(): Int = synchronized(lock) {
        return BottomViewsVisibilityMapper.calculatePriorityContainerId(flags)
    }

    fun getState(): BottomViewsVisibilityState = synchronized(lock) {
        return _state.value
    }

    private fun emitStateLocked() {
        val priorityId = BottomViewsVisibilityMapper.calculatePriorityContainerId(flags)
        val map = HashMap<Int, Float>(32)
        for (i in 0 until 32) {
            val v = inMemoryVisibilities[i]
            if (v > 0f) {
                map[i] = v
            }
        }
        if (!map.containsKey(priorityId) && (flags and (1 shl priorityId)) != 0) {
            map[priorityId] = 1.0f
        }

        _state.value = BottomViewsVisibilityMapper.toState(
            flags = flags,
            priorityId = priorityId,
            visibilities = map
        )
    }
}
