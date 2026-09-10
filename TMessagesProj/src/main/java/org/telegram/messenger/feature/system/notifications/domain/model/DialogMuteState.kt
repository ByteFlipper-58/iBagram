package org.telegram.messenger.feature.system.notifications.domain.model

data class DialogMuteState(
    val dialogId: Long,
    val topicId: Long = 0,
    val isMuted: Boolean = false,
    val muteUntil: Int = 0
)
