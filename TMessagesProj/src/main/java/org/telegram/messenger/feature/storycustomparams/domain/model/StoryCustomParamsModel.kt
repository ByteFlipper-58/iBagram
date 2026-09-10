package org.telegram.messenger.feature.storycustomparams.domain.model

/**
 * Pure domain model representing story translation custom parameters.
 */
data class StoryTranslationParamsModel(
    val isTranslated: Boolean = false,
    val detectedLanguage: String? = null,
    val translatedText: String? = null,
    val translatedLanguage: String? = null
) {
    fun isEmpty(): Boolean =
        !isTranslated &&
        detectedLanguage.isNullOrEmpty() &&
        translatedText.isNullOrEmpty() &&
        translatedLanguage.isNullOrEmpty()

    fun computeFlags(): Int {
        var flags = 0
        if (isTranslated) flags = flags or 1
        if (!detectedLanguage.isNullOrEmpty()) flags = flags or 2
        if (!translatedText.isNullOrEmpty()) flags = flags or 4
        if (!translatedLanguage.isNullOrEmpty()) flags = flags or 8
        return flags
    }
}

/**
 * Pure domain model representing local custom parameters for a story item.
 */
data class StoryCustomParamsModel(
    val storyId: Int,
    val dialogId: Long,
    val translation: StoryTranslationParamsModel = StoryTranslationParamsModel(),
    val flags: Int = translation.computeFlags(),
    val version: Int = 1
) {
    fun isEmpty(): Boolean = translation.isEmpty()
}

/**
 * Aggregated reactive state of local story custom parameters.
 */
data class StoryCustomParamsState(
    val paramsByStoryKey: Map<Pair<Long, Int>, StoryCustomParamsModel> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
