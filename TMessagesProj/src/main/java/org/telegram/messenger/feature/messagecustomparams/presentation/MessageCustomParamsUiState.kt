package org.telegram.messenger.feature.messagecustomparams.presentation

import org.telegram.messenger.feature.messagecustomparams.domain.model.MessageCustomParamsModel
import org.telegram.messenger.feature.messagecustomparams.domain.model.MessageCustomParamsState

data class MessageCustomParamsUiState(
    val state: MessageCustomParamsState = MessageCustomParamsState(),
    val currentParams: MessageCustomParamsModel? = null,
    val isTranscriptionOpen: Boolean = false,
    val isSummaryOpen: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)
