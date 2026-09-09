package org.telegram.messenger.feature.autodelete.presentation

import org.telegram.messenger.feature.autodelete.domain.model.AutoDeleteTtlModel

sealed interface AutoDeleteUiState {
    data object Initial : AutoDeleteUiState
    data object Loading : AutoDeleteUiState
    data class Success(
        val globalTtl: AutoDeleteTtlModel = AutoDeleteTtlModel.OFF,
        val isSaving: Boolean = false,
        val error: String? = null
    ) : AutoDeleteUiState
    data class Error(val message: String) : AutoDeleteUiState
}
