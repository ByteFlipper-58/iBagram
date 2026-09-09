package org.telegram.messenger.feature.gallerysave.domain.model

/**
 * Peer category target for saving media to gallery.
 */
enum class GallerySavePeerType(val flag: Int, val prefKey: String) {
    PEER(1, "user"),
    GROUP(2, "groups"),
    CHANNEL(4, "channels");

    companion object {
        fun fromFlag(flag: Int): GallerySavePeerType? {
            return entries.firstOrNull { it.flag == flag }
        }

        fun fromPrefKey(key: String): GallerySavePeerType? {
            return entries.firstOrNull { it.prefKey == key }
        }
    }
}
