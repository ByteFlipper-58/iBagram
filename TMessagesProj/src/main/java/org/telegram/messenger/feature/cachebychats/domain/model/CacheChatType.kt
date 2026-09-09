package org.telegram.messenger.feature.cachebychats.domain.model

/**
 * Peer category type for cache retention policies.
 */
enum class CacheChatType(val rawType: Int) {
    USER(0),
    GROUP(1),
    CHANNEL(2),
    STORIES(3);

    companion object {
        fun fromRaw(raw: Int): CacheChatType {
            return entries.firstOrNull { it.rawType == raw } ?: USER
        }
    }
}
