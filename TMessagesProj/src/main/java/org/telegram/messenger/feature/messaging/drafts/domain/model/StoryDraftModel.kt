package org.telegram.messenger.feature.messaging.drafts.domain.model

/**
 * Domain model representing a persistent story or media draft.
 */
data class StoryDraftModel(
    val id: Long,
    val date: Long = System.currentTimeMillis(),
    val type: DraftType = DraftType.NEW,
    val filePath: String? = null,
    val isVideo: Boolean = false,
    val caption: String? = null,
    val isCollage: Boolean = false,
    val editStoryId: Int = 0,
    val editStoryPeerId: Long = 0L,
    val editExpireDate: Long = 0L
) {
    val isEdit: Boolean
        get() = type == DraftType.EDIT || editStoryId != 0

    val isFailed: Boolean
        get() = type == DraftType.FAILED

    fun isExpired(now: Long, expirationPeriodMs: Long = 7L * 24 * 3600 * 1000L): Boolean {
        return if (isEdit && editExpireDate > 0L) {
            now > editExpireDate
        } else {
            now - date > expirationPeriodMs
        }
    }
}
