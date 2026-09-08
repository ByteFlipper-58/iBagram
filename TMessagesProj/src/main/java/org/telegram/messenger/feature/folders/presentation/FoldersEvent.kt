package org.telegram.messenger.feature.folders.presentation

/**
 * Single-shot UI events emitted by [FoldersViewModel].
 */
sealed interface FoldersEvent {
    data class FolderCreated(val folderId: Int) : FoldersEvent
    data class FolderUpdated(val folderId: Int) : FoldersEvent
    data class FolderDeleted(val folderId: Int) : FoldersEvent
    object FoldersReordered : FoldersEvent
    data class ShowError(val message: String) : FoldersEvent
}
