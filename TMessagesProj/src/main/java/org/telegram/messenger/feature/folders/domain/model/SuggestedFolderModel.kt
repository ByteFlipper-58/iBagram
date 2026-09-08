package org.telegram.messenger.feature.folders.domain.model

/**
 * Pure Kotlin immutable model representing a suggested chat folder.
 */
data class SuggestedFolderModel(
    val id: Int,
    val description: String,
    val folder: FolderModel
)
