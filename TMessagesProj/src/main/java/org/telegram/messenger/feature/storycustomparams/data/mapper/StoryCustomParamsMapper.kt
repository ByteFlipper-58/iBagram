package org.telegram.messenger.feature.storycustomparams.data.mapper

import org.telegram.messenger.feature.storycustomparams.domain.model.StoryCustomParamsModel
import org.telegram.messenger.feature.storycustomparams.domain.model.StoryTranslationParamsModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_stories

/**
 * Maps between Telegram's TL_stories.StoryItem and pure domain StoryCustomParamsModel.
 */
object StoryCustomParamsMapper {

    fun mapToDomain(dialogId: Long, storyItem: TL_stories.StoryItem): StoryCustomParamsModel {
        val translation = StoryTranslationParamsModel(
            isTranslated = storyItem.translated,
            detectedLanguage = storyItem.detectedLng,
            translatedText = storyItem.translatedText?.text,
            translatedLanguage = storyItem.translatedLng
        )
        return StoryCustomParamsModel(
            storyId = storyItem.id,
            dialogId = dialogId,
            translation = translation,
            flags = translation.computeFlags(),
            version = 1
        )
    }

    fun applyToStoryItem(domain: StoryCustomParamsModel, storyItem: TL_stories.StoryItem) {
        val t = domain.translation
        storyItem.translated = t.isTranslated
        storyItem.detectedLng = t.detectedLanguage
        storyItem.translatedLng = t.translatedLanguage
        if (t.translatedText != null) {
            val textWithEntities = TLRPC.TL_textWithEntities()
            textWithEntities.text = t.translatedText
            storyItem.translatedText = textWithEntities
        } else {
            storyItem.translatedText = null
        }
    }
}
