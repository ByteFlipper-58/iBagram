package org.telegram.messenger.feature.storycustomparams.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.storycustomparams.domain.model.StoryCustomParamsModel
import org.telegram.messenger.feature.storycustomparams.domain.model.StoryCustomParamsState
import org.telegram.messenger.feature.storycustomparams.domain.model.StoryTranslationParamsModel
import org.telegram.messenger.feature.storycustomparams.domain.repository.StoryCustomParamsRepository
import org.telegram.ui.Stories.StoriesStorage
import org.telegram.ui.Stories.StoryCustomParamsHelper

/**
 * Thread-safe implementation of StoryCustomParamsRepository adapting legacy StoryCustomParamsHelper.
 */
class LegacyStoryCustomParamsRepository(
    private val currentAccount: Int
) : StoryCustomParamsRepository {

    private val _state = MutableStateFlow(StoryCustomParamsState())

    private val storiesStorage: StoriesStorage?
        get() = try {
            org.telegram.messenger.MessagesController.getInstance(currentAccount)?.storiesController?.storiesStorage
        } catch (_: Throwable) {
            null
        }

    override fun observeState(): StateFlow<StoryCustomParamsState> = _state.asStateFlow()

    override fun getState(): StoryCustomParamsState = _state.value

    override fun getParams(dialogId: Long, storyId: Int): StoryCustomParamsModel? {
        val key = dialogId to storyId
        return _state.value.paramsByStoryKey[key]
    }

    override fun saveParams(params: StoryCustomParamsModel) {
        val key = params.dialogId to params.storyId
        val updatedFlags = params.translation.computeFlags()
        val normalized = params.copy(flags = updatedFlags)

        _state.update { curr ->
            curr.copy(paramsByStoryKey = curr.paramsByStoryKey + (key to normalized))
        }
    }

    override fun updateTranslation(
        dialogId: Long,
        storyId: Int,
        isTranslated: Boolean,
        detectedLang: String?,
        translatedText: String?,
        targetLang: String?
    ) {
        val key = dialogId to storyId
        _state.update { curr ->
            val existing = curr.paramsByStoryKey[key] ?: StoryCustomParamsModel(
                storyId = storyId,
                dialogId = dialogId
            )
            val updatedTranslation = StoryTranslationParamsModel(
                isTranslated = isTranslated,
                detectedLanguage = detectedLang ?: existing.translation.detectedLanguage,
                translatedText = translatedText ?: existing.translation.translatedText,
                translatedLanguage = targetLang ?: existing.translation.translatedLanguage
            )
            val updated = existing.copy(
                translation = updatedTranslation,
                flags = updatedTranslation.computeFlags()
            )
            curr.copy(paramsByStoryKey = curr.paramsByStoryKey + (key to updated))
        }
    }

    override fun copyParams(fromDialogId: Long, fromStoryId: Int, toDialogId: Long, toStoryId: Int) {
        val fromKey = fromDialogId to fromStoryId
        val toKey = toDialogId to toStoryId

        _state.update { curr ->
            val source = curr.paramsByStoryKey[fromKey]
            if (source != null) {
                val copied = source.copy(
                    dialogId = toDialogId,
                    storyId = toStoryId
                )
                curr.copy(paramsByStoryKey = curr.paramsByStoryKey + (toKey to copied))
            } else {
                curr
            }
        }
    }

    override fun removeParams(dialogId: Long, storyId: Int) {
        val key = dialogId to storyId
        _state.update { curr ->
            curr.copy(paramsByStoryKey = curr.paramsByStoryKey - key)
        }
    }

    override fun clearAll() {
        _state.update { StoryCustomParamsState() }
    }
}
