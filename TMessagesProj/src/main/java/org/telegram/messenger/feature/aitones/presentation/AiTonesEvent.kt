package org.telegram.messenger.feature.aitones.presentation

import org.telegram.messenger.feature.aitones.domain.model.AiToneModel

sealed class AiTonesEvent {
    object LoadTones : AiTonesEvent()
    object RefreshTones : AiTonesEvent()
    data class SelectTone(val tone: AiToneModel?) : AiTonesEvent()
    data class AddTone(val tone: AiToneModel) : AiTonesEvent()
    data class RemoveTone(val tone: AiToneModel) : AiTonesEvent()
    data class UnsaveTone(val tone: AiToneModel) : AiTonesEvent()
    data class EditTone(val tone: AiToneModel) : AiTonesEvent()
    object ClearError : AiTonesEvent()
}
