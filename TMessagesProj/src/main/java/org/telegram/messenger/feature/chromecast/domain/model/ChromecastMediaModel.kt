package org.telegram.messenger.feature.chromecast.domain.model

data class ChromecastMediaModel(
    val mimeType: String,
    val title: String? = null,
    val subtitle: String? = null,
    val externalPath: String? = null,
    val width: Int = 0,
    val height: Int = 0
) {
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")

    val isAudio: Boolean
        get() = mimeType.startsWith("audio/")

    val isImage: Boolean
        get() = mimeType.startsWith("image/")
}
