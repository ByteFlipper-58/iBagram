package org.telegram.messenger.feature.drafts.data.mapper

import org.telegram.messenger.feature.drafts.domain.model.DraftType
import org.telegram.messenger.feature.drafts.domain.model.StoryDraftModel
import org.telegram.ui.Stories.recorder.StoryEntry

object DraftsMapper {

    fun toDomain(entry: StoryEntry): StoryDraftModel {
        val draftType = when {
            entry.isError -> DraftType.FAILED
            entry.isEdit -> DraftType.EDIT
            else -> DraftType.NEW
        }

        return StoryDraftModel(
            id = entry.draftId,
            date = entry.draftDate,
            type = draftType,
            filePath = entry.file?.absolutePath,
            isVideo = entry.isVideo,
            caption = entry.caption?.toString(),
            isCollage = entry.isCollage,
            editStoryId = entry.editStoryId,
            editStoryPeerId = entry.editStoryPeerId,
            editExpireDate = entry.editExpireDate
        )
    }
}
