package org.telegram.messenger.feature.aitones.presentation

import org.telegram.messenger.feature.aitones.domain.model.AiToneModel
import org.telegram.messenger.feature.aitones.domain.model.AiTonesStateModel

sealed class AiTonesUiState {
    object Initial : AiTonesUiState()
    object Loading : AiTonesUiState()
    data class Success(
        val state: AiTonesStateModel,
        val selectedTone: AiToneModel? = null,
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
    ) : AiTonesUiState()
    data class Error(val message: String) : AiTonesUiState()
}
