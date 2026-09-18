package org.telegram.messenger

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.collection.LongSparseArray
import org.telegram.messenger.SendMessagesHelper.ImportingHistory
import org.telegram.messenger.SendMessagesHelper.ImportingSticker
import org.telegram.messenger.SendMessagesHelper.ImportingStickers
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Controller responsible for third-party chat history importing and sticker pack importing.
 * Extracted from SendMessagesHelper as part of Phase 4 (Batch 4.7) modularization.
 */
class HistoryImportController(currentAccount: Int) : BaseController(currentAccount) {

    val importingHistoryFiles = HashMap<String, ImportingHistory>()
    val importingHistoryMap = LongSparseArray<ImportingHistory>()

    val importingStickersFiles = HashMap<String, ImportingStickers>()
    val importingStickersMap = HashMap<String, ImportingStickers>()

    fun getImportingStickers(shortName: String?): ImportingStickers? {
        if (shortName == null) return null
        return importingStickersMap[shortName]
    }

    fun getImportingHistory(dialogId: Long): ImportingHistory? {
        return importingHistoryMap[dialogId]
    }

    fun isImportingStickers(): Boolean {
        return importingStickersMap.isNotEmpty()
    }

    fun isImportingStickers(shortName: String?): Boolean {
        if (shortName == null) return false
        return importingStickersMap.containsKey(shortName)
    }

    fun isImportingHistory(): Boolean {
        return importingHistoryMap.size() != 0
    }

    fun isImportingHistory(dialogId: Long): Boolean {
        return importingHistoryMap[dialogId] != null
    }

    fun putImportingHistory(dialogId: Long, history: ImportingHistory) {
        importingHistoryMap.put(dialogId, history)
    }

    fun removeImportingHistory(dialogId: Long): ImportingHistory? {
        val prev = importingHistoryMap[dialogId]
        importingHistoryMap.remove(dialogId)
        return prev
    }

    fun putImportingStickers(shortName: String, stickers: ImportingStickers) {
        importingStickersMap[shortName] = stickers
    }

    fun removeImportingStickers(shortName: String): ImportingStickers? {
        return importingStickersMap.remove(shortName)
    }

    fun registerHistoryFile(path: String, history: ImportingHistory) {
        importingHistoryFiles[path] = history
    }

    fun registerStickerFile(path: String, stickers: ImportingStickers) {
        importingStickersFiles[path] = stickers
    }

    fun onFileUploadProgress(fileName: String, loadedSize: Long, totalSize: Long) {
        val history = importingHistoryFiles[fileName]
        if (history != null) {
            history.addUploadProgress(fileName, loadedSize, loadedSize / totalSize.toFloat())
        }
        val stickers = importingStickersFiles[fileName]
        if (stickers != null) {
            stickers.addUploadProgress(fileName, loadedSize, loadedSize / totalSize.toFloat())
        }
    }

    fun onFileLoaded(location: String, file: TLRPC.InputFile, mediaImportId: Long? = null) {
        val history = importingHistoryFiles[location]
        if (history != null) {
            if (location == history.historyPath) {
                history.initImport(file)
            } else {
                history.onMediaImport(location, mediaImportId ?: 0L, file)
            }
        }
        val stickers = importingStickersFiles[location]
        if (stickers != null) {
            stickers.onMediaImport(location, mediaImportId ?: 0L, file)
        }
    }

    fun onFileFailedToUpload(location: String) {
        val history = importingHistoryFiles[location]
        history?.onFileFailedToUpload(location)
        val stickers = importingStickersFiles[location]
        stickers?.onFileFailedToUpload(location)
    }

    fun cleanup() {
        importingHistoryFiles.clear()
        importingHistoryMap.clear()
        importingStickersFiles.clear()
        importingStickersMap.clear()
    }

    fun prepareImportHistory(
        dialogId: Long,
        uri: Uri,
        mediaUris: ArrayList<Uri>?,
        onStartImport: MessagesStorage.LongCallback
    ) {
        if (importingHistoryMap[dialogId] != null) {
            onStartImport.run(0)
            return
        }
        if (DialogObject.isChatDialog(dialogId)) {
            val chat = messagesController.getChat(-dialogId)
            if (chat != null && !chat.megagroup) {
                messagesController.convertToMegaGroup(null, -dialogId, null) { chatId ->
                    if (chatId != 0L) {
                        prepareImportHistory(-chatId, uri, mediaUris, onStartImport)
                    } else {
                        onStartImport.run(0)
                    }
                }
                return
            }
        }
        Thread {
            val uris = mediaUris ?: ArrayList()
            val importingHistory = sendMessagesHelper.createImportingHistory()
            importingHistory.mediaPaths = uris
            importingHistory.dialogId = dialogId
            importingHistory.peer = messagesController.getInputPeer(dialogId)
            val files = HashMap<String, ImportingHistory>()
            for (a in 0 until uris.size + 1) {
                val mediaUri = if (a == 0) uri else uris[a - 1]
                if (mediaUri == null || AndroidUtilities.isInternalUri(mediaUri)) {
                    if (a == 0) {
                        AndroidUtilities.runOnUIThread { onStartImport.run(0) }
                        return@Thread
                    }
                    continue
                }

                var ext = "txt"
                val filename = FileLoader.fixFileName(MediaController.getFileName(uri))
                if (filename != null && filename.endsWith(".zip")) {
                    ext = "zip"
                }

                var path = MediaController.copyFileToCache(mediaUri, ext)
                if ("zip" == ext && path != null) {
                    val zipfile = File(path)
                    try {
                        ZipInputStream(FileInputStream(zipfile)).use { zis ->
                            var zipEntry: ZipEntry? = zis.nextEntry
                            while (zipEntry != null) {
                                var name = zipEntry.name
                                if (name != null) {
                                    val idx = name.lastIndexOf("/")
                                    if (idx >= 0) {
                                        name = name.substring(idx + 1)
                                    }
                                    if (name.endsWith(".txt")) {
                                        val newFile = MediaController.createFileInCache(name, "txt")
                                        path = newFile.absolutePath
                                        FileOutputStream(newFile).use { fos ->
                                            val buffer = ByteArray(1024)
                                            var len: Int
                                            while (zis.read(buffer).also { len = it } > 0) {
                                                fos.write(buffer, 0, len)
                                            }
                                        }
                                        break
                                    }
                                }
                                zipEntry = zis.nextEntry
                            }
                            zis.closeEntry()
                        }
                    } catch (e: IOException) {
                        FileLog.e(e)
                    } catch (e2: Exception) {
                        FileLog.e(e2)
                    }
                    try {
                        zipfile.delete()
                    } catch (e: Exception) {
                        FileLog.e(e)
                    }
                }
                if (path == null) {
                    continue
                }
                val f = File(path)
                val size = if (f.exists()) f.length() else 0L
                if (!f.exists() || size == 0L) {
                    if (a == 0) {
                        AndroidUtilities.runOnUIThread { onStartImport.run(0) }
                        return@Thread
                    }
                    continue
                }
                importingHistory.totalSize += size
                if (a == 0) {
                    if (size > 32 * 1024 * 1024) {
                        f.delete()
                        AndroidUtilities.runOnUIThread {
                            Toast.makeText(
                                ApplicationLoader.applicationContext,
                                LocaleController.getString(R.string.ImportFileTooLarge),
                                Toast.LENGTH_SHORT
                            ).show()
                            onStartImport.run(0)
                        }
                        return@Thread
                    }
                    importingHistory.historyPath = path
                } else {
                    importingHistory.uploadMedia.add(path)
                }
                importingHistory.uploadSet.add(path)
                files[path] = importingHistory
            }
            AndroidUtilities.runOnUIThread {
                importingHistoryFiles.putAll(files)
                importingHistoryMap.put(dialogId, importingHistory)
                fileLoader.uploadFile(importingHistory.historyPath, false, true, 0, ConnectionsManager.FileTypeFile, true)
                notificationCenter.postNotificationName(NotificationCenter.historyImportProgressChanged, dialogId)
                onStartImport.run(dialogId)

                val intent = Intent(ApplicationLoader.applicationContext, ImportingService::class.java)
                try {
                    ApplicationLoader.applicationContext?.startService(intent)
                } catch (e: Throwable) {
                    FileLog.e(e)
                }
            }
        }.start()
    }

    fun prepareImportStickers(
        title: String,
        shortName: String,
        software: String,
        paths: ArrayList<ImportingSticker>,
        onStartImport: MessagesStorage.StringCallback
    ) {
        if (importingStickersMap[shortName] != null) {
            onStartImport.run(null)
            return
        }
        Thread {
            val importingStickers = sendMessagesHelper.createImportingStickers()
            importingStickers.title = title
            importingStickers.shortName = shortName
            importingStickers.software = software
            val files = HashMap<String, ImportingStickers>()
            for (a in 0 until paths.size) {
                val sticker = paths[a]
                val f = File(sticker.path)
                val size = if (f.exists()) f.length() else 0L
                if (!f.exists() || size == 0L) {
                    if (a == 0) {
                        AndroidUtilities.runOnUIThread { onStartImport.run(null) }
                        return@Thread
                    }
                    continue
                }
                importingStickers.totalSize += size
                importingStickers.uploadMedia.add(sticker)
                importingStickers.uploadSet[sticker.path] = sticker
                files[sticker.path] = importingStickers
            }
            AndroidUtilities.runOnUIThread {
                if (importingStickers.uploadMedia.isNotEmpty() && importingStickers.uploadMedia[0].item != null) {
                    importingStickers.startImport()
                } else {
                    importingStickersFiles.putAll(files)
                    importingStickersMap[shortName] = importingStickers
                    importingStickers.initImport()
                    notificationCenter.postNotificationName(NotificationCenter.historyImportProgressChanged, shortName)
                    onStartImport.run(shortName)
                }
            }
        }.start()
    }

    companion object {
        private val Instance = arrayOfNulls<HistoryImportController>(UserConfig.MAX_ACCOUNT_COUNT)
        private val lockObjects = Array(UserConfig.MAX_ACCOUNT_COUNT) { Any() }

        @JvmStatic
        fun getInstance(num: Int): HistoryImportController {
            var localInstance = Instance[num]
            if (localInstance == null) {
                synchronized(lockObjects[num]) {
                    localInstance = Instance[num]
                    if (localInstance == null) {
                        localInstance = HistoryImportController(num)
                        Instance[num] = localInstance
                    }
                }
            }
            return localInstance!!
        }
    }
}
