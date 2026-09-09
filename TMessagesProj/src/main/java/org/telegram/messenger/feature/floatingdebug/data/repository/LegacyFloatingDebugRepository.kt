package org.telegram.messenger.feature.floatingdebug.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.feature.floatingdebug.data.mapper.FloatingDebugMapper
import org.telegram.messenger.feature.floatingdebug.domain.model.DebugItemModel
import org.telegram.messenger.feature.floatingdebug.domain.model.FloatingDebugState
import org.telegram.messenger.feature.floatingdebug.domain.repository.FloatingDebugRepository
import org.telegram.ui.Components.FloatingDebug.FloatingDebugController
import org.telegram.ui.LaunchActivity

class LegacyFloatingDebugRepository(
    private val activityProvider: (() -> LaunchActivity?)? = null
) : FloatingDebugRepository {

    private val lock = Any()
    private var inMemoryActive: Boolean = false
    private val inMemoryItems = ArrayList<DebugItemModel>()

    private val _stateFlow = MutableStateFlow(FloatingDebugState.DEFAULT)

    override fun isActive(): Boolean {
        synchronized(lock) {
            return if (ApplicationLoader.applicationContext != null) {
                FloatingDebugController.isActive()
            } else {
                inMemoryActive
            }
        }
    }

    override fun setActive(active: Boolean, saveConfig: Boolean) {
        synchronized(lock) {
            inMemoryActive = active
            if (ApplicationLoader.applicationContext != null) {
                val activity = activityProvider?.invoke()
                if (activity != null) {
                    FloatingDebugController.setActive(activity, active, saveConfig)
                } else if (saveConfig) {
                    SharedConfig.isFloatingDebugActive = active
                    SharedConfig.saveConfig()
                }
            }
            emitStateLocked()
        }
    }

    override fun toggleActive(saveConfig: Boolean): Boolean {
        synchronized(lock) {
            val nextState = !isActive()
            setActive(nextState, saveConfig)
            return nextState
        }
    }

    override fun getDebugItems(): List<DebugItemModel> {
        synchronized(lock) {
            return ArrayList(inMemoryItems)
        }
    }

    override fun registerDebugItems(items: List<DebugItemModel>) {
        synchronized(lock) {
            inMemoryItems.addAll(items)
            emitStateLocked()
        }
    }

    override fun clearDebugItems() {
        synchronized(lock) {
            inMemoryItems.clear()
            emitStateLocked()
        }
    }

    override fun getState(): FloatingDebugState {
        synchronized(lock) {
            return _stateFlow.value
        }
    }

    override fun observeState(): Flow<FloatingDebugState> {
        return _stateFlow.asStateFlow()
    }

    private fun emitStateLocked() {
        val active = if (ApplicationLoader.applicationContext != null) {
            FloatingDebugController.isActive()
        } else {
            inMemoryActive
        }

        _stateFlow.value = FloatingDebugMapper.toState(
            isActive = active,
            items = ArrayList(inMemoryItems)
        )
    }
}
