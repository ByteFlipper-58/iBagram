package org.telegram.messenger.feature.chromecast.domain.model

data class ChromecastStateModel(
    val isCasting: Boolean = false,
    val deviceName: String? = null,
    val currentMedia: ChromecastMediaModel? = null,
    val isConnected: Boolean = false
)
