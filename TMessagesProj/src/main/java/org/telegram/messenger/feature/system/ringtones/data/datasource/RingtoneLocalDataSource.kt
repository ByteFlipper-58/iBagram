package org.telegram.messenger.feature.system.ringtones.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.MediaDataController
import org.telegram.messenger.feature.system.ringtones.data.mapper.RingtoneMapper
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneLimitsModel
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneModel
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Local data source managing notification sounds, ringtone cache, and ongoing sound conversions.
 */
open class RingtoneLocalDataSource(
    private val currentAccount: Int = 0
) {
    private var isTestMode: Boolean = false
    private val lock = Any()
    private val localIdGenerator = AtomicLong(1000L)
    private val activeUploads = ConcurrentHashMap<String, Long>()

    private val _state = MutableStateFlow(
        RingtoneState(
            ringtones = emptyList(),
            selectedRingtoneId = null,
            isLoading = false,
            limits = RingtoneLimitsModel()
        )
    )

    init {
        loadFromLegacy()
    }

    fun setTestMode(initialState: RingtoneState = RingtoneState()) {
        isTestMode = true
        _state.value = initialState
    }

    private fun loadFromLegacy() {
        if (isTestMode) return
        try {
            val controller = MediaDataController.getInstance(currentAccount)
            val ringtoneStore = controller.ringtoneDataStore
            if (ringtoneStore != null) {
                val list = ringtoneStore.userRingtones
                if (list != null) {
                    val mapped = list.mapNotNull { tone ->
                        RingtoneMapper.mapCachedToneToModel(tone)
                    }
                    _state.value = _state.value.copy(ringtones = mapped)
                }
            }
        } catch (_: Throwable) {
            // Keep default in unit tests / headless
        }
    }

    open fun observeRingtones(): Flow<List<RingtoneModel>> {
        return _state.map { it.ringtones }
    }

    open fun observeState(): Flow<RingtoneState> = _state.asStateFlow()

    open fun getState(): RingtoneState = _state.value

    open fun getRingtones(): List<RingtoneModel> = _state.value.ringtones

    open fun getRingtoneById(id: Long): RingtoneModel? {
        return _state.value.ringtones.firstOrNull { it.id == id }
    }

    open fun getRingtoneSoundPath(id: Long): String? {
        val tone = getRingtoneById(id) ?: return null
        return tone.localUri ?: "telegram_sound_${tone.id}"
    }

    open fun addRingtone(ringtone: RingtoneModel) {
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

    open fun removeRingtone(id: Long): Boolean {
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

    open fun saveRingtoneFromDocument(
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

    open fun uploadRingtone(
        filePath: String,
        fileName: String,
        durationSec: Int,
        sizeBytes: Long
    ): kotlin.Result<RingtoneModel> {
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

        return kotlin.Result.success(pendingTone)
    }

    open fun completeUpload(filePath: String, finalDocumentId: Long) {
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

    open fun cancelUpload(filePath: String) {
        synchronized(lock) {
            val toneId = activeUploads.remove(filePath) ?: return
            val current = _state.value.ringtones.toMutableList()
            current.removeAll { it.id == toneId }
            _state.value = _state.value.copy(ringtones = current)
        }
    }

    open fun refreshRingtones(force: Boolean) {
        synchronized(lock) {
            _state.value = _state.value.copy(isLoading = false)
        }
        if (!isTestMode && force) {
            loadFromLegacy()
        }
    }

    open fun selectRingtone(id: Long?) {
        synchronized(lock) {
            _state.value = _state.value.copy(selectedRingtoneId = id)
        }
    }

    open fun getLimits(): RingtoneLimitsModel = _state.value.limits
}
