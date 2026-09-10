package org.telegram.messenger.feature.flagsecure.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.flagsecure.domain.model.SecurityReasonType
import org.telegram.messenger.feature.flagsecure.domain.model.WindowSecurityState
import org.telegram.messenger.feature.flagsecure.domain.repository.FlagSecureRepository
import java.util.concurrent.ConcurrentHashMap

class LegacyFlagSecureRepository(
    private val onWindowStateChanged: ((windowId: String, isSecured: Boolean) -> Unit)? = null
) : FlagSecureRepository {

    private val windowReasons = ConcurrentHashMap<String, ConcurrentHashMap<SecurityReasonType, () -> Boolean>>()
    private val windowFlows = ConcurrentHashMap<String, MutableStateFlow<WindowSecurityState>>()
    private val allWindowsFlow = MutableStateFlow<Map<String, WindowSecurityState>>(emptyMap())

    override fun attachReason(
        windowId: String,
        reason: SecurityReasonType,
        condition: (() -> Boolean)?
    ): WindowSecurityState {
        val reasonsMap = windowReasons.computeIfAbsent(windowId) { ConcurrentHashMap() }
        reasonsMap[reason] = condition ?: { true }
        return recomputeWindowState(windowId)
    }

    override fun detachReason(
        windowId: String,
        reason: SecurityReasonType
    ): WindowSecurityState {
        val reasonsMap = windowReasons[windowId]
        reasonsMap?.remove(reason)
        return recomputeWindowState(windowId)
    }

    override fun invalidateWindow(windowId: String): WindowSecurityState {
        return recomputeWindowState(windowId)
    }

    override fun isWindowSecured(windowId: String): Boolean {
        return getWindowState(windowId).isSecured
    }

    override fun getWindowState(windowId: String): WindowSecurityState {
        return windowFlows[windowId]?.value ?: WindowSecurityState(windowId = windowId)
    }

    override fun getAllWindowStates(): Map<String, WindowSecurityState> {
        return allWindowsFlow.value
    }

    override fun resetWindow(windowId: String) {
        windowReasons.remove(windowId)
        val resetState = WindowSecurityState(
            windowId = windowId,
            isSecured = false,
            activeReasonsCount = 0,
            activeReasons = emptyList(),
            lastUpdatedMs = System.currentTimeMillis()
        )
        windowFlows[windowId]?.value = resetState
        allWindowsFlow.update { current ->
            current.toMutableMap().apply { remove(windowId) }
        }
        onWindowStateChanged?.invoke(windowId, false)
    }

    override fun observeWindowState(windowId: String): StateFlow<WindowSecurityState> {
        return getOrCreateFlow(windowId).asStateFlow()
    }

    override fun observeAllWindowStates(): StateFlow<Map<String, WindowSecurityState>> {
        return allWindowsFlow.asStateFlow()
    }

    private fun getOrCreateFlow(windowId: String): MutableStateFlow<WindowSecurityState> {
        return windowFlows.computeIfAbsent(windowId) {
            MutableStateFlow(WindowSecurityState(windowId = windowId))
        }
    }

    private fun recomputeWindowState(windowId: String): WindowSecurityState {
        val reasonsMap = windowReasons[windowId]
        val activeReasons = mutableListOf<SecurityReasonType>()

        if (reasonsMap != null) {
            for ((reason, condition) in reasonsMap) {
                val isActive = condition?.invoke() ?: true
                if (isActive) {
                    activeReasons.add(reason)
                }
            }
        }

        val flow = getOrCreateFlow(windowId)
        val previousSecured = flow.value.isSecured
        val newState = WindowSecurityState(
            windowId = windowId,
            isSecured = activeReasons.isNotEmpty(),
            activeReasonsCount = activeReasons.size,
            activeReasons = activeReasons,
            lastUpdatedMs = System.currentTimeMillis()
        )

        flow.value = newState
        allWindowsFlow.update { current ->
            val updated = current.toMutableMap()
            if (newState.isSecured || newState.activeReasonsCount > 0) {
                updated[windowId] = newState
            } else {
                updated.remove(windowId)
            }
            updated
        }

        if (previousSecured != newState.isSecured) {
            onWindowStateChanged?.invoke(windowId, newState.isSecured)
        }

        return newState
    }
}
