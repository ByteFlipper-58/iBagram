package org.telegram.messenger.feature.aitones.data.mapper

import org.telegram.messenger.AiTonesController
import org.telegram.messenger.feature.aitones.domain.model.AiToneModel
import org.telegram.messenger.feature.aitones.domain.model.AiTonesStateModel
import org.telegram.tgnet.tl.TL_aicompose

object AiToneMapper {

    fun mapTone(tone: TL_aicompose.AiComposeTone?): AiToneModel? {
        if (tone == null) return null
        return when (tone) {
            is TL_aicompose.TL_aiComposeTone -> AiToneModel(
                id = tone.id,
                title = tone.title ?: "",
                emojiDocumentId = if (tone.emoji_id != 0L) tone.emoji_id else null,
                slug = tone.slug,
                prompt = tone.prompt,
                isCreator = tone.creator,
                installsCount = tone.installs_count,
                isDefault = false,
                defaultToneKey = null,
                exampleFromText = tone.example_english?.from?.text,
                exampleToText = tone.example_english?.to?.text,
            )
            is TL_aicompose.TL_aiComposeToneDefault -> AiToneModel(
                id = null,
                title = tone.title ?: "",
                emojiDocumentId = if (tone.emoji_id != 0L) tone.emoji_id else null,
                slug = null,
                prompt = null,
                isCreator = false,
                installsCount = 0,
                isDefault = true,
                defaultToneKey = tone.tone,
                exampleFromText = null,
                exampleToText = null,
            )
            else -> AiToneModel(
                id = null,
                title = tone.title ?: "",
                emojiDocumentId = if (tone.emoji_id != 0L) tone.emoji_id else null,
                isDefault = false,
            )
        }
    }

    fun mapTonesList(tones: List<TL_aicompose.AiComposeTone>?): List<AiToneModel> {
        if (tones == null) return emptyList()
        return tones.mapNotNull { mapTone(it) }
    }

    fun mapState(controller: AiTonesController?): AiTonesStateModel {
        if (controller == null) return AiTonesStateModel()
        return AiTonesStateModel(
            tones = mapTonesList(controller.tones),
            savedCount = controller.savedTonesCount,
            isLoading = controller.isLoading,
            hash = controller.hash,
        )
    }

    fun findMatchingTone(controller: AiTonesController, model: AiToneModel): TL_aicompose.AiComposeTone? {
        for (t in controller.tones) {
            if (model.id != null && t is TL_aicompose.TL_aiComposeTone && t.id == model.id) {
                return t
            }
            if (model.defaultToneKey != null && t is TL_aicompose.TL_aiComposeToneDefault && t.tone == model.defaultToneKey) {
                return t
            }
            if (t.title == model.title) {
                return t
            }
        }
        return null
    }
}
