package org.telegram.messenger.feature.media.chromecast.presentation

import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastStateModel

data class ChromecastUiState(
    val isLoading: Boolean = false,
    val state: ChromecastStateModel = ChromecastStateModel(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
)
