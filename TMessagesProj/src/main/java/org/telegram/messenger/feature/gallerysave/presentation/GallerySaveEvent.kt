package org.telegram.messenger.feature.gallerysave.presentation

import org.telegram.messenger.feature.gallerysave.domain.model.GallerySaveDialogExceptionModel
import org.telegram.messenger.feature.gallerysave.domain.model.GallerySavePeerType

/**
 * MVI Events for GallerySaveViewModel.
 */
sealed interface GallerySaveEvent {
    data class SelectPeerType(val peerType: GallerySavePeerType) : GallerySaveEvent
    data class TogglePhoto(val peerType: GallerySavePeerType) : GallerySaveEvent
    data class ToggleVideo(val peerType: GallerySavePeerType) : GallerySaveEvent
    data class TogglePeer(val peerType: GallerySavePeerType) : GallerySaveEvent
    data class SetVideoLimit(val peerType: GallerySavePeerType, val limitBytes: Long) : GallerySaveEvent
    data class SetException(val peerType: GallerySavePeerType, val exception: GallerySaveDialogExceptionModel) : GallerySaveEvent
    data class RemoveException(val peerType: GallerySavePeerType, val dialogId: Long) : GallerySaveEvent
    data class RemoveAllExceptions(val peerType: GallerySavePeerType) : GallerySaveEvent
    object DismissMessages : GallerySaveEvent
}
