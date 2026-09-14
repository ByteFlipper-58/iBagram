package org.telegram.messenger.feature.media.fileloader.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.FileLoader
import org.telegram.messenger.feature.media.fileloader.domain.model.FileTransferModel
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class FileLoaderLocalDataSource(
    private val currentAccount: Int = 0
) {
    private val transfersMap = ConcurrentHashMap<String, FileTransferModel>()
    private val _transfersFlow = MutableStateFlow<List<FileTransferModel>>(emptyList())
    val transfersFlow: StateFlow<List<FileTransferModel>> = _transfersFlow.asStateFlow()

    private val customPaths = ConcurrentHashMap<String, String>()

    fun getTransfer(id: String): FileTransferModel? = transfersMap[id]

    fun updateTransfer(model: FileTransferModel) {
        transfersMap[model.id] = model
        _transfersFlow.value = transfersMap.values.toList()
    }

    fun removeTransfer(id: String): FileTransferModel? {
        val removed = transfersMap.remove(id)
        if (removed != null) {
            _transfersFlow.value = transfersMap.values.toList()
        }
        return removed
    }

    fun clearTransfers() {
        transfersMap.clear()
        _transfersFlow.value = emptyList()
    }

    fun setCustomLocalPath(fileName: String, path: String) {
        customPaths[fileName] = path
    }

    fun resolveLocalPath(fileName: String): String? {
        val custom = customPaths[fileName]
        if (custom != null) {
            return custom
        }

        val transfer = transfersMap[fileName]
        if (transfer?.filePath != null && File(transfer.filePath).exists()) {
            return transfer.filePath
        }

        try {
            val dirTypes = intArrayOf(
                FileLoader.MEDIA_DIR_CACHE,
                FileLoader.MEDIA_DIR_FILES,
                FileLoader.MEDIA_DIR_IMAGE,
                FileLoader.MEDIA_DIR_VIDEO,
                FileLoader.MEDIA_DIR_AUDIO
            )
            for (type in dirTypes) {
                val dir = FileLoader.checkDirectory(type)
                if (dir != null) {
                    val file = File(dir, fileName)
                    if (file.exists()) {
                        return file.absolutePath
                    }
                }
            }
        } catch (_: Throwable) {
            // JVM fallback without native directory checks
        }

        return null
    }
}
