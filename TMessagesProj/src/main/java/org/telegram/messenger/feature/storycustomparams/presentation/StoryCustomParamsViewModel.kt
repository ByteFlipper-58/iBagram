package org.telegram.messenger.feature.storycustomparams.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.storycustomparams.domain.usecase.CheckStoryCustomParamsEmptyUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.CopyStoryCustomParamsUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.GetStoryCustomParamsUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.ObserveStoryCustomParamsStateUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.RemoveStoryCustomParamsUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.SaveStoryCustomParamsUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.UpdateStoryTranslationUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.ClearAllStoryCustomParamsUseCase

/**
 * MVI ViewModel for managing story custom parameters and translation states.
 */
class StoryCustomParamsViewModel(
    private val observeStateUseCase: ObserveStoryCustomParamsStateUseCase,
    private val getParamsUseCase: GetStoryCustomParamsUseCase,
    private val saveParamsUseCase: SaveStoryCustomParamsUseCase,
    private val updateTranslationUseCase: UpdateStoryTranslationUseCase,
    private val copyParamsUseCase: CopyStoryCustomParamsUseCase,
    private val removeParamsUseCase: RemoveStoryCustomParamsUseCase,
    private val clearAllUseCase: ClearAllStoryCustomParamsUseCase,
    private val checkEmptyUseCase: CheckStoryCustomParamsEmptyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryCustomParamsUiState())
    val uiState: StateFlow<StoryCustomParamsUiState> = _uiState.asStateFlow()

    init {
        observeStateUseCase()
            .onEach { domainState ->
                _uiState.update { curr ->
                    val selected = if (curr.selectedDialogId != null && curr.selectedStoryId != null) {
                        domainState.paramsByStoryKey[curr.selectedDialogId to curr.selectedStoryId]
                    } else {
                        curr.selectedParams
                    }
                    curr.copy(
                        state = domainState,
                        selectedParams = selected,
                        errorMessage = domainState.errorMessage
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: StoryCustomParamsEvent) {
        when (event) {
            is StoryCustomParamsEvent.SelectStory -> {
                val params = getParamsUseCase(event.dialogId, event.storyId)
                _uiState.update {
                    it.copy(
                        selectedDialogId = event.dialogId,
                        selectedStoryId = event.storyId,
                        selectedParams = params,
                        isTranslationVisible = params?.translation?.isTranslated == true,
                        errorMessage = null
                    )
                }
            }
            is StoryCustomParamsEvent.SaveParams -> {
                saveParamsUseCase(event.params)
                _uiState.update { it.copy(errorMessage = null) }
            }
            is StoryCustomParamsEvent.UpdateTranslation -> {
                updateTranslationUseCase(
                    dialogId = event.dialogId,
                    storyId = event.storyId,
                    isTranslated = event.isTranslated,
                    detectedLang = event.detectedLang,
                    translatedText = event.translatedText,
                    targetLang = event.targetLang
                )
                _uiState.update { it.copy(errorMessage = null) }
            }
            is StoryCustomParamsEvent.CopyParams -> {
                copyParamsUseCase(
                    fromDialogId = event.fromDialogId,
                    fromStoryId = event.fromStoryId,
                    toDialogId = event.toDialogId,
                    toStoryId = event.toStoryId
                )
                _uiState.update { it.copy(errorMessage = null) }
            }
            is StoryCustomParamsEvent.DeleteParams -> {
                removeParamsUseCase(event.dialogId, event.storyId)
                if (_uiState.value.selectedDialogId == event.dialogId && _uiState.value.selectedStoryId == event.storyId) {
                    _uiState.update { it.copy(selectedParams = null, isTranslationVisible = false) }
                }
            }
            is StoryCustomParamsEvent.ToggleTranslationVisibility -> {
                _uiState.update { it.copy(isTranslationVisible = !it.isTranslationVisible) }
            }
            is StoryCustomParamsEvent.ClearAll -> {
                clearAllUseCase()
                _uiState.update { StoryCustomParamsUiState() }
            }
            is StoryCustomParamsEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }
}
