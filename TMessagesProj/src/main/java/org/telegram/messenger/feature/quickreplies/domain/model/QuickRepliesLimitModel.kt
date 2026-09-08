package org.telegram.messenger.feature.quickreplies.domain.model

data class QuickRepliesLimitModel(
    val currentCount: Int,
    val maxLimit: Int,
    val canAddNew: Boolean
)
