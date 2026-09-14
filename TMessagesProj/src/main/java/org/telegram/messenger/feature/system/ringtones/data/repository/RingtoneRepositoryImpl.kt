package org.telegram.messenger.feature.system.ringtones.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.ringtones.data.datasource.RingtoneLocalDataSource
import org.telegram.messenger.feature.system.ringtones.data.datasource.RingtoneRemoteDataSource
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneLimitsModel
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneModel
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneState
import org.telegram.messenger.feature.system.ringtones.domain.repository.RingtoneRepository

/**
 * Implementation of [RingtoneRepository] coordinating local notification tone storage
 * and remote MTProto ringtone document synchronization.
 */
class RingtoneRepositoryImpl(
    private val localDataSource: RingtoneLocalDataSource,
    private val remoteDataSource: RingtoneRemoteDataSource
) : RingtoneRepository {

    override fun observeRingtones(): Flow<List<RingtoneModel>> {
        return localDataSource.observeRingtones()
    }

    override fun observeState(): Flow<RingtoneState> {
        return localDataSource.observeState()
    }

    override fun getState(): RingtoneState {
        return localDataSource.getState()
    }

    override fun getRingtones(): List<RingtoneModel> {
        return localDataSource.getRingtones()
    }

    override fun getRingtoneById(id: Long): RingtoneModel? {
        return localDataSource.getRingtoneById(id)
    }

    override fun getRingtoneSoundPath(id: Long): String? {
        return localDataSource.getRingtoneSoundPath(id)
    }

    override fun addRingtone(ringtone: RingtoneModel) {
        localDataSource.addRingtone(ringtone)
    }

    override fun removeRingtone(id: Long): Boolean {
        return localDataSource.removeRingtone(id)
    }

    override fun saveRingtoneFromDocument(
        documentId: Long,
        title: String,
        durationSec: Int,
        sizeBytes: Long,
        mimeType: String
    ): Boolean {
        return localDataSource.saveRingtoneFromDocument(
            documentId = documentId,
            title = title,
            durationSec = durationSec,
            sizeBytes = sizeBytes,
            mimeType = mimeType
        )
    }

    override fun uploadRingtone(
        filePath: String,
        fileName: String,
        durationSec: Int,
        sizeBytes: Long
    ): Result<RingtoneModel> {
        return localDataSource.uploadRingtone(filePath, fileName, durationSec, sizeBytes)
    }

    override fun cancelUpload(filePath: String) {
        localDataSource.cancelUpload(filePath)
    }

    override fun refreshRingtones(force: Boolean) {
        localDataSource.refreshRingtones(force)
    }

    override fun selectRingtone(id: Long?) {
        localDataSource.selectRingtone(id)
    }

    override fun getLimits(): RingtoneLimitsModel {
        return localDataSource.getLimits()
    }
}
