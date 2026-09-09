package org.telegram.messenger.feature.hashtagsearch.domain.model

data class HashtagMessageModel(
    val id: Int,
    val realId: Int,
    val dialogId: Long,
    val text: String,
    val date: Long,
    val chatTitle: String? = null,
    val isGroupPrimary: Boolean = false
)
