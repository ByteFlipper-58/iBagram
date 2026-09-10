package org.telegram.messenger.feature.messaging.drafts.presentation

import org.telegram.messenger.feature.messaging.drafts.domain.model.DraftType
import org.telegram.messenger.feature.messaging.drafts.domain.model.StoryDraftModel

/**
 * Events for the story and media drafts screen/recorder.
 */
sealed class DraftsEvent {
    data class Load(val force: Boolean = false) : DraftsEvent()
    data class Save(val draft: StoryDraftModel) : DraftsEvent()
    data class Delete(val draftId: Long) : DraftsEvent()
    data class DeleteMultiple(val draftIds: List<Long>) : DraftsEvent()
    data class DeleteForEdit(val peerId: Long, val storyId: Int) : DraftsEvent()
    data class SelectDraft(val draft: StoryDraftModel?) : DraftsEvent()
    data class FilterByType(val type: DraftType?) : DraftsEvent()
    data class CleanupExpired(val now: Long = System.currentTimeMillis()) : DraftsEvent()
    object DismissInfo : DraftsEvent()
}
