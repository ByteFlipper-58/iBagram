package org.telegram.messenger.feature.flagsecure.presentation

import org.telegram.messenger.feature.flagsecure.domain.model.SecurityReasonType
import org.telegram.messenger.feature.flagsecure.domain.model.WindowSecurityState

data class FlagSecureUiState(
    val currentWindowId: String = "main",
    val windowState: WindowSecurityState = WindowSecurityState(windowId = "main"),
    val allWindows: Map<String, WindowSecurityState> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isSecured: Boolean
        get() = windowState.isSecured

    val activeReasonsCount: Int
        get() = windowState.activeReasonsCount

    val activeReasons: List<SecurityReasonType>
        get() = windowState.activeReasons

    val securedWindowsCount: Int
        get() = allWindows.values.count { it.isSecured }
}
