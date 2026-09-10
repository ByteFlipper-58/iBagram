package org.telegram.messenger.feature.system.ringtones.presentation

import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneLimitsModel
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneModel

data class RingtoneUiState(
    val ringtones: List<RingtoneModel> = emptyList(),
    val selectedRingtoneId: Long? = null,
    val previewPlayingId: Long? = null,
    val isLoading: Boolean = false,
    val activeUploadsCount: Int = 0,
    val limits: RingtoneLimitsModel = RingtoneLimitsModel(),
    val error: String? = null
)
