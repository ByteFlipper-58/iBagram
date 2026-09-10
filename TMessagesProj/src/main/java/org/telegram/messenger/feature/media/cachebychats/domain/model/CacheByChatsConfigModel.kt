package org.telegram.messenger.feature.media.cachebychats.domain.model

/**
 * Composite domain configuration of keep-media durations and chat exceptions.
 */
data class CacheByChatsConfigModel(
    val userDuration: KeepMediaDuration = KeepMediaDuration.FOREVER,
    val groupDuration: KeepMediaDuration = KeepMediaDuration.ONE_MONTH,
    val channelDuration: KeepMediaDuration = KeepMediaDuration.ONE_WEEK,
    val storiesDuration: KeepMediaDuration = KeepMediaDuration.TWO_DAY,
    val exceptions: List<KeepMediaExceptionModel> = emptyList()
) {
    fun getDurationForType(type: CacheChatType): KeepMediaDuration {
        return when (type) {
            CacheChatType.USER -> userDuration
            CacheChatType.GROUP -> groupDuration
            CacheChatType.CHANNEL -> channelDuration
            CacheChatType.STORIES -> storiesDuration
        }
    }

    fun getExceptionsForType(type: CacheChatType): List<KeepMediaExceptionModel> {
        return exceptions.filter { it.type == type }
    }

    fun findException(dialogId: Long): KeepMediaExceptionModel? {
        return exceptions.firstOrNull { it.dialogId == dialogId }
    }
}
