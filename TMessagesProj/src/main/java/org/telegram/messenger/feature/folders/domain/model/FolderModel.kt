package org.telegram.messenger.feature.folders.domain.model

/**
 * Pure Kotlin immutable domain model representing a chat folder / filter tab.
 */
data class FolderModel(
    val id: Int,
    val name: String,
    val unreadCount: Int = 0,
    val order: Int = 0,
    val flags: Int = 0,
    val color: Int = -1,
    val isDefault: Boolean = false,
    val isChatlist: Boolean = false,
    val isLocked: Boolean = false,
    val includedPeerIds: List<Long> = emptyList(),
    val excludedPeerIds: List<Long> = emptyList(),
    val pinnedPeerIds: List<Long> = emptyList()
)
