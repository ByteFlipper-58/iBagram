package org.telegram.messenger.feature.messaging.drafts.presentation

import org.telegram.messenger.feature.messaging.drafts.domain.model.DraftType
import org.telegram.messenger.feature.messaging.drafts.domain.model.StoryDraftModel

/**
 * UI State for managing drafts in Story recorder and gallery.
 */
data class DraftsUiState(
    val isLoading: Boolean = false,
    val drafts: List<StoryDraftModel> = emptyList(),
    val selectedDraft: StoryDraftModel? = null,
    val filterType: DraftType? = null,
    val infoMessage: String? = null
) {
    val displayedDrafts: List<StoryDraftModel>
        get() = if (filterType == null) drafts else drafts.filter { it.type == filterType }

    val hasDrafts: Boolean
        get() = drafts.isNotEmpty()

    val totalCount: Int
        get() = drafts.size
}
