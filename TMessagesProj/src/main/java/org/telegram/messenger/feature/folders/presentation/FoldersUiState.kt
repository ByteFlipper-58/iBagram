package org.telegram.messenger.feature.folders.presentation

import org.telegram.messenger.feature.folders.domain.model.FolderModel
import org.telegram.messenger.feature.folders.domain.model.SuggestedFolderModel

/**
 * Pure Kotlin UI state for chat folders / filter tabs.
 */
sealed interface FoldersUiState {
    object Loading : FoldersUiState

    data class Success(
        val folders: List<FolderModel>,
        val suggested: List<SuggestedFolderModel> = emptyList()
    ) : FoldersUiState

    data class Error(val message: String?) : FoldersUiState
}
