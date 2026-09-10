package org.telegram.messenger.feature.system.windowvisibility.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.system.windowvisibility.data.mapper.WindowVisibilityMapper
import org.telegram.messenger.feature.system.windowvisibility.domain.model.WindowVisibilityController
import org.telegram.messenger.feature.system.windowvisibility.domain.model.WindowVisibilityState
import org.telegram.messenger.feature.system.windowvisibility.domain.repository.WindowVisibilityRepository
import org.telegram.messenger.utils.WindowVisibilityManager

/**
 * Thread-safe repository implementing [WindowVisibilityRepository].
 * Adapts legacy [WindowVisibilityManager] logic with modern reactive flows.
 */
class LegacyWindowVisibilityRepository(
    private val legacyListener: WindowVisibilityManager.OnVisibilityChangedListener? = null
) : WindowVisibilityRepository {

    private val lock = Any()
    private val activeReasons = mutableSetOf<String>()
    private var reasonsCount = 0

    private val _stateFlow = MutableStateFlow(
        WindowVisibilityMapper.toState(
            reasonsCount = 0,
            activeReasons = emptySet()
        )
    )
    val stateFlow: StateFlow<WindowVisibilityState> = _stateFlow.asStateFlow()

    private val _visibilityFlow = MutableSharedFlow<Boolean>(replay = 1)
    val visibilityFlow: Flow<Boolean> = _visibilityFlow.asSharedFlow()

    init {
        _visibilityFlow.tryEmit(true)
    }

    override fun requestHide(reasonTag: String, description: String): WindowVisibilityState {
        val (newState, toggled) = synchronized(lock) {
            val previousState = _stateFlow.value
            val added = activeReasons.add(reasonTag)
            if (added) {
                reasonsCount++
            }
            val state = WindowVisibilityMapper.toState(
                reasonsCount = reasonsCount,
                activeReasons = activeReasons,
                lastChangedReason = reasonTag
            )
            _stateFlow.value = state
            val result = WindowVisibilityMapper.calculateChangeResult(previousState, state)
            Pair(state, result.visibilityToggled)
        }

        if (toggled) {
            _visibilityFlow.tryEmit(newState.isVisible)
            legacyListener?.onVisibilityChanged(newState.isVisible)
        }
        return newState
    }

    override fun releaseHide(reasonTag: String): WindowVisibilityState {
        val (newState, toggled) = synchronized(lock) {
            val previousState = _stateFlow.value
            val removed = activeReasons.remove(reasonTag)
            if (removed) {
                reasonsCount = (reasonsCount - 1).coerceAtLeast(0)
            }
            val state = WindowVisibilityMapper.toState(
                reasonsCount = reasonsCount,
                activeReasons = activeReasons,
                lastChangedReason = reasonTag
            )
            _stateFlow.value = state
            val result = WindowVisibilityMapper.calculateChangeResult(previousState, state)
            Pair(state, result.visibilityToggled)
        }

        if (toggled) {
            _visibilityFlow.tryEmit(newState.isVisible)
            legacyListener?.onVisibilityChanged(newState.isVisible)
        }
        return newState
    }

    override fun toggleHide(reasonTag: String, hide: Boolean, description: String): WindowVisibilityState {
        return if (hide) {
            requestHide(reasonTag, description)
        } else {
            releaseHide(reasonTag)
        }
    }

    override fun isVisible(): Boolean {
        synchronized(lock) {
            return _stateFlow.value.isVisible
        }
    }

    override fun isHidden(): Boolean {
        synchronized(lock) {
            return _stateFlow.value.isHidden
        }
    }

    override fun getReasonsCount(): Int {
        synchronized(lock) {
            return reasonsCount
        }
    }

    override fun getActiveReasons(): Set<String> {
        synchronized(lock) {
            return activeReasons.toSet()
        }
    }

    override fun getCurrentState(): WindowVisibilityState {
        synchronized(lock) {
            return _stateFlow.value
        }
    }

    override fun resetAllReasons(): WindowVisibilityState {
        val (newState, toggled) = synchronized(lock) {
            val previousState = _stateFlow.value
            activeReasons.clear()
            reasonsCount = 0
            val state = WindowVisibilityMapper.toState(
                reasonsCount = 0,
                activeReasons = emptySet(),
                lastChangedReason = "reset_all"
            )
            _stateFlow.value = state
            val result = WindowVisibilityMapper.calculateChangeResult(previousState, state)
            Pair(state, result.visibilityToggled)
        }

        if (toggled) {
            _visibilityFlow.tryEmit(newState.isVisible)
            legacyListener?.onVisibilityChanged(newState.isVisible)
        }
        return newState
    }

    override fun observeState(): StateFlow<WindowVisibilityState> {
        return stateFlow
    }

    override fun observeVisibilityChanges(): Flow<Boolean> {
        return visibilityFlow
    }

    override fun obtainController(reasonTag: String): WindowVisibilityController {
        return ControllerImpl(reasonTag)
    }

    private inner class ControllerImpl(
        override val reasonTag: String
    ) : WindowVisibilityController {

        private var _hidden = false
        private var _destroyed = false

        override val isHidden: Boolean
            get() = synchronized(lock) { _hidden }

        override val isDestroyed: Boolean
            get() = synchronized(lock) { _destroyed }

        override fun setHidden(hidden: Boolean) {
            synchronized(lock) {
                if (_destroyed || _hidden == hidden) return
                _hidden = hidden
            }
            if (hidden) {
                requestHide(reasonTag)
            } else {
                releaseHide(reasonTag)
            }
        }

        override fun destroy() {
            setHidden(false)
            synchronized(lock) {
                _destroyed = true
            }
        }
    }
}
