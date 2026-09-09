package org.telegram.messenger.feature.aitones.domain.model

/**
 * Domain representation of an AI Compose Tone.
 * Decoupled from [org.telegram.tgnet.tl.TL_aicompose.AiComposeTone].
 */
data class AiToneModel(
    val id: Long? = null,
    val title: String,
    val emojiDocumentId: Long? = null,
    val slug: String? = null,
    val prompt: String? = null,
    val isCreator: Boolean = false,
    val installsCount: Int = 0,
    val isDefault: Boolean = false,
    val defaultToneKey: String? = null,
    val exampleFromText: String? = null,
    val exampleToText: String? = null,
)
