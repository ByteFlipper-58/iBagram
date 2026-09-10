package org.telegram.messenger.feature.network.proxy.presentation

import org.telegram.messenger.feature.network.proxy.domain.model.ProxyModel
import org.telegram.messenger.feature.network.proxy.domain.model.ProxySettingsModel

sealed interface ProxyUiState {
    data object Initial : ProxyUiState
    data object Loading : ProxyUiState
    data class Success(
        val settings: ProxySettingsModel = ProxySettingsModel(),
        val checkingProxy: ProxyModel? = null,
        val isSaving: Boolean = false,
        val error: String? = null
    ) : ProxyUiState
    data class Error(val message: String) : ProxyUiState
}
