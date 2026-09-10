package org.telegram.messenger.feature.system.ringtones.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.feature.system.ringtones.data.mapper.RingtoneMapper
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneLimitsModel
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneModel
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneState
import org.telegram.messenger.feature.system.ringtones.domain.repository.RingtoneRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class LegacyRingtoneRepository(
    private val currentAccount: Int = 0
) : RingtoneRepository {

    private val lock = Any()
    private val localIdGenerator = AtomicLong(1000L)
    private val activeUploads = ConcurrentHashMap<String, Long>() // filePath -> ringtoneId

    private val _state = MutableStateFlow(
        RingtoneState(
            ringtones = emptyList(),
            selectedRingtoneId = null,
            isLoading = false,
            limits = RingtoneLimitsModel()
        )
    )

    override fun observeRingtones(): Flow<List<RingtoneModel>> {
        return _state.map { it.ringtones }
    }

    override fun observeState(): Flow<RingtoneState> = _state.asStateFlow()

    override fun getState(): RingtoneState = _state.value

    override fun getRingtones(): List<RingtoneModel> = _state.value.ringtones

    override fun getRingtoneById(id: Long): RingtoneModel? {
        return _state.value.ringtones.firstOrNull { it.id == id }
    }

    override fun getRingtoneSoundPath(id: Long): String? {
        val tone = getRingtoneById(id) ?: return null
        return tone.localUri ?: "telegram_sound_${tone.id}"
    }

    override fun addRingtone(ringtone: RingtoneModel) {
        synchronized(lock) {
            val current = _state.value.ringtones.toMutableList()
            val existingIndex = current.indexOfFirst { it.id == ringtone.id }
            if (existingIndex >= 0) {
                current[existingIndex] = ringtone
            } else {
                current.add(ringtone)
            }
            _state.value = _state.value.copy(ringtones = current)
        }
    }

    override fun removeRingtone(id: Long): Boolean {
        return synchronized(lock) {
            val current = _state.value.ringtones.toMutableList()
            val removed = current.removeAll { it.id == id }
            if (removed) {
                val newSelected = if (_state.value.selectedRingtoneId == id) null else _state.value.selectedRingtoneId
                _state.value = _state.value.copy(
                    ringtones = current,
                    selectedRingtoneId = newSelected
                )
            }
            removed
        }
    }

    override fun saveRingtoneFromDocument(
        documentId: Long,
        title: String,
        durationSec: Int,
        sizeBytes: Long,
        mimeType: String
    ): Boolean {
        synchronized(lock) {
            val current = _state.value.ringtones.toMutableList()
            if (current.any { it.id == documentId }) {
                return true
            }
            val model = RingtoneModel(
                id = documentId,
                title = title,
                durationSec = durationSec,
                sizeBytes = sizeBytes,
                mimeType = mimeType,
                localUri = null,
                isUploading = false,
                isConverting = false
            )
            current.add(model)
            _state.value = _state.value.copy(ringtones = current)
            return true
        }
    }

    override fun uploadRingtone(
        filePath: String,
        fileName: String,
        durationSec: Int,
        sizeBytes: Long
    ): Result<RingtoneModel> {
        val toneId = localIdGenerator.incrementAndGet()
        val title = RingtoneMapper.extractTitleFromFileName(fileName)
        val ext = fileName.substringAfterLast('.', "")
        val mimeType = RingtoneMapper.resolveMimeTypeFromExtension(ext)

        val pendingTone = RingtoneModel(
            id = toneId,
            title = title,
            durationSec = durationSec,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            localUri = filePath,
            isUploading = true
        )

        synchronized(lock) {
            activeUploads[filePath] = toneId
            val current = _state.value.ringtones.toMutableList()
            current.add(pendingTone)
            _state.value = _state.value.copy(ringtones = current)
        }

        return Result.success(pendingTone)
    }

    fun completeUpload(filePath: String, finalDocumentId: Long) {
        synchronized(lock) {
            val toneId = activeUploads.remove(filePath) ?: return
            val current = _state.value.ringtones.toMutableList()
            val index = current.indexOfFirst { it.id == toneId }
            if (index >= 0) {
                val updated = current[index].copy(
                    id = finalDocumentId,
                    isUploading = false
                )
                current[index] = updated
                _state.value = _state.value.copy(ringtones = current)
            }
        }
    }

    override fun cancelUpload(filePath: String) {
        synchronized(lock) {
            val toneId = activeUploads.remove(filePath) ?: return
            val current = _state.value.ringtones.toMutableList()
            current.removeAll { it.id == toneId }
            _state.value = _state.value.copy(ringtones = current)
        }
    }

    override fun refreshRingtones(force: Boolean) {
        synchronized(lock) {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    override fun selectRingtone(id: Long?) {
        synchronized(lock) {
            _state.value = _state.value.copy(selectedRingtoneId = id)
        }
    }

    override fun getLimits(): RingtoneLimitsModel = _state.value.limits
}
