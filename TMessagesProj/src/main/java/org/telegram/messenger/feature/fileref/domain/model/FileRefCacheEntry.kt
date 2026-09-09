package org.telegram.messenger.feature.fileref.domain.model

/**
 * Domain model describing a cached parent response containing refreshed file references.
 */
data class FileRefCacheEntry(
    val parentKey: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileReferenceBytesCount: Int = 0
) {
    fun isExpired(now: Long, ttlMs: Long = 60_000L): Boolean {
        return now - timestamp > ttlMs
    }
}
