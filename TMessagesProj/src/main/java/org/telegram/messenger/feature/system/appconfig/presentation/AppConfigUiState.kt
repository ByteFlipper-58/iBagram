package org.telegram.messenger.feature.system.appconfig.presentation

import org.telegram.messenger.feature.system.appconfig.domain.model.AppGlobalConfigState

data class AppConfigUiState(
    val config: AppGlobalConfigState = AppGlobalConfigState(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdatedTimestamp: Long = 0L
)
