package org.telegram.messenger.feature.media.autodeletemedia.presentation

import org.telegram.messenger.feature.media.autodeletemedia.domain.model.AutoDeleteRunResult
import org.telegram.messenger.feature.media.autodeletemedia.domain.model.AutoDeleteTaskState

data class AutoDeleteMediaUiState(
    val taskState: AutoDeleteTaskState = AutoDeleteTaskState(),
    val isCleaningUp: Boolean = false,
    val lastResult: AutoDeleteRunResult? = null,
    val errorMessage: String? = null
)
