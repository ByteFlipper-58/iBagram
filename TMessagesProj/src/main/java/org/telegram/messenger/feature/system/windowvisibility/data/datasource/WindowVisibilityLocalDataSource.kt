package org.telegram.messenger.feature.system.windowvisibility.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.system.windowvisibility.data.mapper.WindowVisibilityMapper
import org.telegram.messenger.feature.system.windowvisibility.domain.model.WindowVisibilityController
import org.telegram.messenger.feature.system.windowvisibility.domain.model.WindowVisibilityState
import org.telegram.messenger.utils.WindowVisibilityManager

/**
 * Local data source managing window visibility arbitration, reference-counting hide reasons,
 * and reactive state/visibility flows.
 */
open class WindowVisibilityLocalDataSource(
    private val currentAccount: Int = 0,
    private val legacyListener: WindowVisibilityManager.OnVisibilityChangedListener? = null
) {
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

    open fun requestHide(reasonTag: String, description: String = ""): WindowVisibilityState {
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

    open fun releaseHide(reasonTag: String): WindowVisibilityState {
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

    open fun toggleHide(reasonTag: String, hide: Boolean, description: String = ""): WindowVisibilityState {
        return if (hide) {
            requestHide(reasonTag, description)
        } else {
            releaseHide(reasonTag)
        }
    }

    open fun isVisible(): Boolean {
        synchronized(lock) {
            return _stateFlow.value.isVisible
        }
    }

    open fun isHidden(): Boolean {
        synchronized(lock) {
            return _stateFlow.value.isHidden
        }
    }

    open fun getReasonsCount(): Int {
        synchronized(lock) {
            return reasonsCount
        }
    }

    open fun getActiveReasons(): Set<String> {
        synchronized(lock) {
            return activeReasons.toSet()
        }
    }

    open fun getCurrentState(): WindowVisibilityState {
        synchronized(lock) {
            return _stateFlow.value
        }
    }

    open fun resetAllReasons(): WindowVisibilityState {
        val (newState, toggled) = synchronized(lock) {
            val previousState = _stateFlow.value
            activeReasons.clear()
            reasonsCount = 0
            val state = WindowVisibilityMapper.toState(
                reasonsCount = 0,
                activeReasons = emptySet()
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

    open fun obtainController(reasonTag: String): WindowVisibilityController {
        return SubsystemVisibilityController(reasonTag, this)
    }

    private class SubsystemVisibilityController(
        override val reasonTag: String,
        private val localDataSource: WindowVisibilityLocalDataSource
    ) : WindowVisibilityController {

        private var _isHidden = false
        private var _isDestroyed = false

        override val isHidden: Boolean get() = _isHidden
        override val isDestroyed: Boolean get() = _isDestroyed

        override fun setHidden(hidden: Boolean) {
            if (_isDestroyed || _isHidden == hidden) return
            _isHidden = hidden
            if (hidden) {
                localDataSource.requestHide(reasonTag)
            } else {
                localDataSource.releaseHide(reasonTag)
            }
        }

        override fun destroy() {
            if (_isDestroyed) return
            if (_isHidden) {
                localDataSource.releaseHide(reasonTag)
                _isHidden = false
            }
            _isDestroyed = true
        }
    }
}
