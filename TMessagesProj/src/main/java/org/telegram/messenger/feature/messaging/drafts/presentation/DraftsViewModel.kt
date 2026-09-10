package org.telegram.messenger.feature.messaging.drafts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.messaging.drafts.domain.model.DraftsStateModel
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.CleanupExpiredDraftsUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.DeleteDraftUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.DeleteForEditUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.GetDraftForEditUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.GetDraftsStateUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.LoadDraftsUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.ObserveDraftsStateUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.SaveDraftUseCase

class DraftsViewModel(
    private val observeDraftsStateUseCase: ObserveDraftsStateUseCase,
    private val getDraftsStateUseCase: GetDraftsStateUseCase,
    private val loadDraftsUseCase: LoadDraftsUseCase,
    private val saveDraftUseCase: SaveDraftUseCase,
    private val deleteDraftUseCase: DeleteDraftUseCase,
    private val deleteForEditUseCase: DeleteForEditUseCase,
    private val getDraftForEditUseCase: GetDraftForEditUseCase,
    private val cleanupExpiredDraftsUseCase: CleanupExpiredDraftsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DraftsUiState(isLoading = true))
    val uiState: StateFlow<DraftsUiState> = _uiState.asStateFlow()

    init {
        observeDraftsStateUseCase()
            .onEach { state ->
                updateFromState(state)
            }
            .launchIn(viewModelScope)

        onEvent(DraftsEvent.Load())
    }

    fun onEvent(event: DraftsEvent) {
        when (event) {
            is DraftsEvent.Load -> {
                viewModelScope.launch {
                    loadDraftsUseCase(event.force)
                }
            }
            is DraftsEvent.Save -> {
                viewModelScope.launch {
                    saveDraftUseCase(event.draft)
                }
            }
            is DraftsEvent.Delete -> {
                viewModelScope.launch {
                    deleteDraftUseCase(event.draftId)
                }
            }
            is DraftsEvent.DeleteMultiple -> {
                viewModelScope.launch {
                    deleteDraftUseCase(event.draftIds)
                }
            }
            is DraftsEvent.DeleteForEdit -> {
                viewModelScope.launch {
                    deleteForEditUseCase(event.peerId, event.storyId)
                }
            }
            is DraftsEvent.SelectDraft -> {
                _uiState.value = _uiState.value.copy(selectedDraft = event.draft)
            }
            is DraftsEvent.FilterByType -> {
                _uiState.value = _uiState.value.copy(filterType = event.type)
            }
            is DraftsEvent.CleanupExpired -> {
                viewModelScope.launch {
                    val removed = cleanupExpiredDraftsUseCase(event.now)
                    if (removed.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            infoMessage = "Cleaned up ${removed.size} expired drafts"
                        )
                    }
                }
            }
            is DraftsEvent.DismissInfo -> {
                _uiState.value = _uiState.value.copy(infoMessage = null)
            }
        }
    }

    private fun updateFromState(state: DraftsStateModel) {
        _uiState.value = _uiState.value.copy(
            isLoading = state.isLoading,
            drafts = state.drafts
        )
    }
}
