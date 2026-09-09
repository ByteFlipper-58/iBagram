package org.telegram.messenger.feature.drafts.domain.model

/**
 * Domain model representing the aggregated drafts state.
 */
data class DraftsStateModel(
    val drafts: List<StoryDraftModel> = emptyList(),
    val isLoading: Boolean = false,
    val lastCleanedTimestamp: Long = 0L
) {
    val totalCount: Int
        get() = drafts.size

    val newDraftsCount: Int
        get() = drafts.count { it.type == DraftType.NEW }

    val editDraftsCount: Int
        get() = drafts.count { it.type == DraftType.EDIT }

    val failedDraftsCount: Int
        get() = drafts.count { it.type == DraftType.FAILED }
}
