package org.telegram.messenger.feature.media.fileref.domain.model

/**
 * Domain model describing a pending or in-flight file reference renewal request.
 */
data class FileRefRequestItem(
    val locationKey: String,
    val parentKey: String,
    val parentType: FileRefParentType = FileRefParentType.CUSTOM,
    val parentId: Long = 0L,
    val requestTime: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)
