package org.telegram.messenger.feature.messaging.folders.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.folders.domain.model.FolderModel
import org.telegram.messenger.feature.messaging.folders.domain.usecase.CreateFolderUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.DeleteFolderUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.GetFolderUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.GetFoldersUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.GetSuggestedFoldersUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.ObserveFoldersUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.ReorderFoldersUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.UpdateFolderUseCase

/**
 * ViewModel managing chat folders list state, suggested folders, and folder operations.
 */
class FoldersViewModel(
    private val observeFoldersUseCase: ObserveFoldersUseCase,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val getFolderUseCase: GetFolderUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val updateFolderUseCase: UpdateFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
    private val reorderFoldersUseCase: ReorderFoldersUseCase,
    private val getSuggestedFoldersUseCase: GetSuggestedFoldersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<FoldersUiState>(FoldersUiState.Loading)
    val uiState: StateFlow<FoldersUiState> = _uiState.asStateFlow()

    private val _events = Channel<FoldersEvent>(Channel.BUFFERED)
    val events: Flow<FoldersEvent> = _events.receiveAsFlow()

    init {
        observeFolders()
        loadSuggestedFolders()
    }

    private fun observeFolders() {
        viewModelScope.launch {
            observeFoldersUseCase().collect { folders ->
                val current = _uiState.value
                val suggested = if (current is FoldersUiState.Success) current.suggested else emptyList()
                _uiState.value = FoldersUiState.Success(
                    folders = folders,
                    suggested = suggested
                )
            }
        }
    }

    fun loadSuggestedFolders() {
        viewModelScope.launch {
            try {
                val suggested = getSuggestedFoldersUseCase()
                val current = _uiState.value
                if (current is FoldersUiState.Success) {
                    _uiState.value = current.copy(suggested = suggested)
                }
            } catch (_: Throwable) {
                // Ignore suggested failures
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val folders = getFoldersUseCase()
                val suggested = getSuggestedFoldersUseCase()
                _uiState.value = FoldersUiState.Success(folders = folders, suggested = suggested)
            } catch (e: Throwable) {
                _uiState.value = FoldersUiState.Error(e.message)
            }
        }
    }

    fun createFolder(
        name: String,
        flags: Int = 0,
        includedPeerIds: List<Long> = emptyList(),
        excludedPeerIds: List<Long> = emptyList(),
        pinnedPeerIds: List<Long> = emptyList(),
        color: Int = -1
    ) {
        viewModelScope.launch {
            when (val result = createFolderUseCase(name, flags, includedPeerIds, excludedPeerIds, pinnedPeerIds, color)) {
                is Result.Success -> _events.send(FoldersEvent.FolderCreated(result.data.id))
                is Result.Failure -> _events.send(FoldersEvent.ShowError(result.error.message))
            }
        }
    }

    fun updateFolder(folder: FolderModel) {
        viewModelScope.launch {
            when (val result = updateFolderUseCase(folder)) {
                is Result.Success -> _events.send(FoldersEvent.FolderUpdated(folder.id))
                is Result.Failure -> _events.send(FoldersEvent.ShowError(result.error.message))
            }
        }
    }

    fun deleteFolder(id: Int) {
        viewModelScope.launch {
            when (val result = deleteFolderUseCase(id)) {
                is Result.Success -> _events.send(FoldersEvent.FolderDeleted(id))
                is Result.Failure -> _events.send(FoldersEvent.ShowError(result.error.message))
            }
        }
    }

    fun reorderFolders(folderIds: List<Int>) {
        viewModelScope.launch {
            when (val result = reorderFoldersUseCase(folderIds)) {
                is Result.Success -> _events.send(FoldersEvent.FoldersReordered)
                is Result.Failure -> _events.send(FoldersEvent.ShowError(result.error.message))
            }
        }
    }
}
