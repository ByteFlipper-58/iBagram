package org.telegram.messenger.feature.media.storycustomparams.presentation

import org.telegram.messenger.feature.media.storycustomparams.domain.model.StoryCustomParamsModel
import org.telegram.messenger.feature.media.storycustomparams.domain.model.StoryCustomParamsState

/**
 * Presentation UI state for story custom parameters and translation overlay.
 */
data class StoryCustomParamsUiState(
    val state: StoryCustomParamsState = StoryCustomParamsState(),
    val selectedDialogId: Long? = null,
    val selectedStoryId: Int? = null,
    val selectedParams: StoryCustomParamsModel? = null,
    val isTranslationVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
