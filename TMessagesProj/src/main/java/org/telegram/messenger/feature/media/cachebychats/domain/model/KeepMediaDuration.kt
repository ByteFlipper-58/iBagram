package org.telegram.messenger.feature.media.cachebychats.domain.model

/**
 * Cache media retention duration options with duration in seconds.
 */
enum class KeepMediaDuration(val rawValue: Int, val seconds: Long) {
    DELETE(4, 0L),
    ONE_DAY(3, 86_400L),
    TWO_DAY(6, 172_800L),
    ONE_WEEK(0, 604_800L),
    ONE_MONTH(1, 2_592_000L),
    FOREVER(2, Long.MAX_VALUE);

    companion object {
        fun fromRaw(raw: Int): KeepMediaDuration {
            return entries.firstOrNull { it.rawValue == raw } ?: FOREVER
        }

        fun defaultForType(type: CacheChatType): KeepMediaDuration {
            return when (type) {
                CacheChatType.USER -> FOREVER
                CacheChatType.GROUP -> ONE_MONTH
                CacheChatType.CHANNEL -> ONE_WEEK
                CacheChatType.STORIES -> TWO_DAY
            }
        }
    }
}
