package org.telegram.messenger.feature.savedmessages.domain.model

/**
 * Pure domain model representing a saved messages tag or reaction category.
 */
data class SavedTagModel(
    val reaction: String,
    val title: String,
    val count: Int
)
