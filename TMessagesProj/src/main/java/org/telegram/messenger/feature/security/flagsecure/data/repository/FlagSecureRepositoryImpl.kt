package org.telegram.messenger.feature.security.flagsecure.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.security.flagsecure.data.datasource.FlagSecureLocalDataSource
import org.telegram.messenger.feature.security.flagsecure.domain.model.SecurityReasonType
import org.telegram.messenger.feature.security.flagsecure.domain.model.WindowSecurityState
import org.telegram.messenger.feature.security.flagsecure.domain.repository.FlagSecureRepository

/**
 * Modern repository implementation for window security arbitration (FLAG_SECURE).
 */
class FlagSecureRepositoryImpl(
    private val localDataSource: FlagSecureLocalDataSource
) : FlagSecureRepository {

    override fun attachReason(
        windowId: String,
        reason: SecurityReasonType,
        condition: (() -> Boolean)?
    ): WindowSecurityState {
        return localDataSource.attachReason(windowId, reason, condition)
    }

    override fun detachReason(
        windowId: String,
        reason: SecurityReasonType
    ): WindowSecurityState {
        return localDataSource.detachReason(windowId, reason)
    }

    override fun invalidateWindow(windowId: String): WindowSecurityState {
        return localDataSource.invalidateWindow(windowId)
    }

    override fun isWindowSecured(windowId: String): Boolean {
        return localDataSource.isWindowSecured(windowId)
    }

    override fun getWindowState(windowId: String): WindowSecurityState {
        return localDataSource.getWindowState(windowId)
    }

    override fun getAllWindowStates(): Map<String, WindowSecurityState> {
        return localDataSource.getAllWindowStates()
    }

    override fun resetWindow(windowId: String) {
        localDataSource.resetWindow(windowId)
    }

    override fun observeWindowState(windowId: String): StateFlow<WindowSecurityState> {
        return localDataSource.observeWindowState(windowId)
    }

    override fun observeAllWindowStates(): StateFlow<Map<String, WindowSecurityState>> {
        return localDataSource.observeAllWindowStates()
    }
}
