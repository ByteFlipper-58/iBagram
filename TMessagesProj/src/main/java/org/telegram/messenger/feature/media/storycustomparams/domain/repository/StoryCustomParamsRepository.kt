package org.telegram.messenger.feature.media.storycustomparams.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.media.storycustomparams.domain.model.StoryCustomParamsModel
import org.telegram.messenger.feature.media.storycustomparams.domain.model.StoryCustomParamsState

/**
 * Pure domain repository contract for story custom parameters and local translation state.
 */
interface StoryCustomParamsRepository {

    /**
     * Observes the reactive state of all locally stored story parameters.
     */
    fun observeState(): StateFlow<StoryCustomParamsState>

    /**
     * Returns an immediate snapshot of the story custom parameters state.
     */
    fun getState(): StoryCustomParamsState

    /**
     * Retrieves local custom parameters for a specific story in a dialog.
     */
    fun getParams(dialogId: Long, storyId: Int): StoryCustomParamsModel?

    /**
     * Persists or updates local custom parameters for a story.
     */
    fun saveParams(params: StoryCustomParamsModel)

    /**
     * Granularly updates translation parameters for a story.
     */
    fun updateTranslation(
        dialogId: Long,
        storyId: Int,
        isTranslated: Boolean,
        detectedLang: String?,
        translatedText: String?,
        targetLang: String?
    )

    /**
     * Copies parameters from one story to another (e.g. when forward/sharing or caching).
     */
    fun copyParams(fromDialogId: Long, fromStoryId: Int, toDialogId: Long, toStoryId: Int)

    /**
     * Removes custom parameters for a specific story.
     */
    fun removeParams(dialogId: Long, storyId: Int)

    /**
     * Clears all in-memory custom parameters.
     */
    fun clearAll()
}
