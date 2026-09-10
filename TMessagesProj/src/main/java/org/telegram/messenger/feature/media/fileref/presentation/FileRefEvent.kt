package org.telegram.messenger.feature.media.fileref.presentation

import org.telegram.messenger.feature.media.fileref.domain.model.FileRefRequestItem

/**
 * Events for managing file reference renewal queue and response caching.
 */
sealed class FileRefEvent {
    data class RequestRenewal(val item: FileRefRequestItem) : FileRefEvent()
    data class NotifyRenewed(val locationKey: String, val parentKey: String, val refLength: Int = 0) : FileRefEvent()
    data class CancelRequest(val locationKey: String) : FileRefEvent()
    object ClearCache : FileRefEvent()
    object DismissInfo : FileRefEvent()
}
