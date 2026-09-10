package org.telegram.messenger.feature.media.storycustomparams.presentation

import org.telegram.messenger.feature.media.storycustomparams.domain.model.StoryCustomParamsModel

/**
 * MVI Events for story custom parameters and translation actions.
 */
sealed class StoryCustomParamsEvent {
    data class SelectStory(val dialogId: Long, val storyId: Int) : StoryCustomParamsEvent()
    data class SaveParams(val params: StoryCustomParamsModel) : StoryCustomParamsEvent()
    data class UpdateTranslation(
        val dialogId: Long,
        val storyId: Int,
        val isTranslated: Boolean,
        val detectedLang: String?,
        val translatedText: String?,
        val targetLang: String?
    ) : StoryCustomParamsEvent()
    data class CopyParams(
        val fromDialogId: Long,
        val fromStoryId: Int,
        val toDialogId: Long,
        val toStoryId: Int
    ) : StoryCustomParamsEvent()
    data class DeleteParams(val dialogId: Long, val storyId: Int) : StoryCustomParamsEvent()
    object ToggleTranslationVisibility : StoryCustomParamsEvent()
    object ClearAll : StoryCustomParamsEvent()
    object DismissError : StoryCustomParamsEvent()
}
