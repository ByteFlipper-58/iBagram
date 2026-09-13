package org.telegram.messenger.feature.media.storycustomparams.data.datasource

import org.telegram.messenger.feature.media.storycustomparams.domain.model.StoryCustomParamsModel
import org.telegram.messenger.feature.media.storycustomparams.domain.model.StoryTranslationParamsModel
import org.telegram.ui.Stories.StoriesStorage
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe local data source for storing and retrieving story custom parameters.
 */
class StoryCustomParamsLocalDataSource(
    private val account: Int
) {
    private val storage = ConcurrentHashMap<Pair<Long, Int>, StoryCustomParamsModel>()

    private val storiesStorage: StoriesStorage?
        get() = try {
            org.telegram.messenger.MessagesController.getInstance(account)?.storiesController?.storiesStorage
        } catch (_: Throwable) {
            null
        }

    fun getParams(dialogId: Long, storyId: Int): StoryCustomParamsModel? {
        return storage[dialogId to storyId]
    }

    fun saveParams(params: StoryCustomParamsModel) {
        val updatedFlags = params.translation.computeFlags()
        val normalized = params.copy(flags = updatedFlags)
        storage[params.dialogId to params.storyId] = normalized
    }

    fun updateTranslation(
        dialogId: Long,
        storyId: Int,
        isTranslated: Boolean,
        detectedLang: String?,
        translatedText: String?,
        targetLang: String?
    ): StoryCustomParamsModel {
        val key = dialogId to storyId
        val existing = storage[key] ?: StoryCustomParamsModel(storyId = storyId, dialogId = dialogId)
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
        storage[key] = updated
        return updated
    }

    fun copyParams(fromDialogId: Long, fromStoryId: Int, toDialogId: Long, toStoryId: Int): StoryCustomParamsModel? {
        val source = storage[fromDialogId to fromStoryId] ?: return null
        val copied = source.copy(dialogId = toDialogId, storyId = toStoryId)
        storage[toDialogId to toStoryId] = copied
        return copied
    }

    fun removeParams(dialogId: Long, storyId: Int): StoryCustomParamsModel? {
        return storage.remove(dialogId to storyId)
    }

    fun clearAll() {
        storage.clear()
    }

    fun getAllParams(): Map<Pair<Long, Int>, StoryCustomParamsModel> {
        return HashMap(storage)
    }
}
