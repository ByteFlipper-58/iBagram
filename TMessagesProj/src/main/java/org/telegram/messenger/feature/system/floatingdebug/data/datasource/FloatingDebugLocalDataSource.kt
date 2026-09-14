package org.telegram.messenger.feature.system.floatingdebug.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.feature.system.floatingdebug.domain.model.DebugItemModel
import org.telegram.messenger.feature.system.floatingdebug.domain.model.FloatingDebugState
import org.telegram.ui.Components.FloatingDebug.FloatingDebugController
import org.telegram.ui.LaunchActivity

/**
 * Local data source managing the floating debug tools overlay, visibility state, and registered debug items.
 */
class FloatingDebugLocalDataSource(
    private val activityProvider: (() -> LaunchActivity?)? = null
) {
    private val lock = Any()
    private var inMemoryActive: Boolean = false
    private val inMemoryItems = ArrayList<DebugItemModel>()
    private var isTestMode: Boolean = false

    private val _stateFlow = MutableStateFlow(FloatingDebugState.DEFAULT)
    val stateFlow: Flow<FloatingDebugState> = _stateFlow.asStateFlow()

    fun setTestMode(isTest: Boolean) {
        this.isTestMode = isTest
    }

    fun isActive(): Boolean = synchronized(lock) {
        if (!isTestMode && ApplicationLoader.applicationContext != null) {
            try {
                FloatingDebugController.isActive()
            } catch (_: Throwable) {
                inMemoryActive
            }
        } else {
            inMemoryActive
        }
    }

    fun setActive(active: Boolean, saveConfig: Boolean = true) = synchronized(lock) {
        inMemoryActive = active
        if (!isTestMode && ApplicationLoader.applicationContext != null) {
            try {
                val activity = activityProvider?.invoke()
                if (activity != null) {
                    FloatingDebugController.setActive(activity, active, saveConfig)
                } else if (saveConfig) {
                    SharedConfig.isFloatingDebugActive = active
                    SharedConfig.saveConfig()
                }
            } catch (_: Throwable) {
                // Ignored in headless tests
            }
        }
        emitStateLocked()
    }

    fun toggleActive(saveConfig: Boolean = true): Boolean = synchronized(lock) {
        val nextState = !isActive()
        setActive(nextState, saveConfig)
        nextState
    }

    fun getDebugItems(): List<DebugItemModel> = synchronized(lock) {
        ArrayList(inMemoryItems)
    }

    fun registerDebugItems(items: List<DebugItemModel>) = synchronized(lock) {
        inMemoryItems.addAll(items)
        emitStateLocked()
    }

    fun clearDebugItems() = synchronized(lock) {
        inMemoryItems.clear()
        emitStateLocked()
    }

    fun getState(): FloatingDebugState = synchronized(lock) {
        _stateFlow.value
    }

    fun observeState(): Flow<FloatingDebugState> = stateFlow

    private fun emitStateLocked() {
        _stateFlow.value = FloatingDebugState(
            isActive = inMemoryActive,
            items = ArrayList(inMemoryItems)
        )
    }
}
