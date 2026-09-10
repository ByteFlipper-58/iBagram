package org.telegram.messenger.feature.ringtones.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.ringtones.domain.model.RingtoneLimitsModel
import org.telegram.messenger.feature.ringtones.domain.model.RingtoneModel
import org.telegram.messenger.feature.ringtones.domain.model.RingtoneState

interface RingtoneRepository {
    fun observeRingtones(): Flow<List<RingtoneModel>>
    fun observeState(): Flow<RingtoneState>
    fun getState(): RingtoneState
    fun getRingtones(): List<RingtoneModel>
    fun getRingtoneById(id: Long): RingtoneModel?
    fun getRingtoneSoundPath(id: Long): String?
    fun addRingtone(ringtone: RingtoneModel)
    fun removeRingtone(id: Long): Boolean
    fun saveRingtoneFromDocument(
        documentId: Long,
        title: String,
        durationSec: Int,
        sizeBytes: Long,
        mimeType: String
    ): Boolean
    fun uploadRingtone(
        filePath: String,
        fileName: String,
        durationSec: Int,
        sizeBytes: Long
    ): Result<RingtoneModel>
    fun cancelUpload(filePath: String)
    fun refreshRingtones(force: Boolean)
    fun selectRingtone(id: Long?)
    fun getLimits(): RingtoneLimitsModel
}
