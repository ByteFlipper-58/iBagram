package org.telegram.messenger.feature.chromecast.presentation

import org.telegram.messenger.feature.chromecast.domain.model.ChromecastMediaModel
import java.io.File

sealed class ChromecastEvent {
    object RefreshState : ChromecastEvent()
    data class CastMedia(val media: ChromecastMediaModel) : ChromecastEvent()
    object StopCasting : ChromecastEvent()
    data class SetCoverFile(val file: File?) : ChromecastEvent()
    object DismissError : ChromecastEvent()
    object DismissInfo : ChromecastEvent()
}
