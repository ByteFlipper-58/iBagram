package org.telegram.messenger.feature.messaging.factcheck.data.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.SQLite.SQLiteCursor
import org.telegram.SQLite.SQLiteDatabase
import org.telegram.SQLite.SQLitePreparedStatement
import org.telegram.messenger.FactCheckController
import org.telegram.messenger.FileLog
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.NotificationCenter
import org.telegram.tgnet.NativeByteBuffer
import org.telegram.tgnet.TLRPC

/**
 * Local data source managing in-memory factcheck caching, MessagesStorage SQLite persistence,
 * InputPeer resolution, and updates processing.
 */
open class FactCheckLocalDataSource(
    protected val currentAccount: Int,
    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    protected val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) {

    open fun getFactCheckFromMemory(hash: Long): TLRPC.TL_factCheck? {
        return try {
            FactCheckController.getInstance(currentAccount).getCachedFactCheck(hash)
        } catch (_: Throwable) {
            null
        }
    }

    open fun putFactCheckToMemory(hash: Long, factCheck: TLRPC.TL_factCheck) {
        try {
            FactCheckController.getInstance(currentAccount).putCachedFactCheck(hash, factCheck)
        } catch (_: Throwable) {
        }
    }

    open suspend fun getFactCheckFromDatabase(hash: Long): TLRPC.TL_factCheck? = withContext(ioDispatcher) {
        var cursor: SQLiteCursor? = null
        try {
            val storage = MessagesStorage.getInstance(currentAccount) ?: return@withContext null
            val db: SQLiteDatabase = storage.database ?: return@withContext null

            cursor = db.queryFinalized("SELECT data FROM fact_checks WHERE hash = ?", hash)
            if (cursor.next()) {
                val data = cursor.byteBufferValue(0)
                val factCheck = TLRPC.TL_factCheck.TLdeserialize(data, data.readInt32(false), false)
                data.reuse()
                return@withContext factCheck
            }
            null
        } catch (e: Throwable) {
            FileLog.e(e)
            null
        } finally {
            try {
                cursor?.dispose()
            } catch (_: Throwable) {
            }
        }
    }

    open suspend fun saveFactCheckToDatabase(factCheck: TLRPC.TL_factCheck) = withContext(ioDispatcher) {
        var state: SQLitePreparedStatement? = null
        try {
            val storage = MessagesStorage.getInstance(currentAccount) ?: return@withContext
            val db: SQLiteDatabase = storage.database ?: return@withContext

            state = db.executeFast("REPLACE INTO fact_checks VALUES(?, ?, ?)")
            state.requery()
            state.bindLong(1, factCheck.hash)
            val buffer = NativeByteBuffer(factCheck.objectSize)
            factCheck.serializeToStream(buffer)
            state.bindByteBuffer(2, buffer)
            state.bindLong(3, System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 60)
            state.step()
            state.dispose()
            state = null
            buffer.reuse()
        } catch (e: Throwable) {
            FileLog.e(e)
        } finally {
            try {
                state?.dispose()
            } catch (_: Throwable) {
            }
        }
    }

    open suspend fun deleteFactCheckFromDatabase(hash: Long) = withContext(ioDispatcher) {
        try {
            val storage = MessagesStorage.getInstance(currentAccount) ?: return@withContext
            val db: SQLiteDatabase = storage.database ?: return@withContext
            db.executeFast("DELETE FROM fact_checks WHERE hash = ?").apply {
                bindLong(1, hash)
                stepThis()
                dispose()
            }
        } catch (e: Throwable) {
            FileLog.e(e)
        }
    }

    open fun getInputPeer(dialogId: Long): TLRPC.InputPeer? {
        return try {
            MessagesController.getInstance(currentAccount).getInputPeer(dialogId)
        } catch (_: Throwable) {
            null
        }
    }

    open fun getFactCheckLimit(): Int {
        return try {
            val limit = MessagesController.getInstance(currentAccount).factcheckLengthLimit
            if (limit > 0) limit else 1024
        } catch (_: Throwable) {
            1024
        }
    }

    open suspend fun processUpdates(updates: TLRPC.Updates) = withContext(mainDispatcher) {
        try {
            MessagesController.getInstance(currentAccount).processUpdates(updates, false)
        } catch (_: Throwable) {
        }
    }

    open fun notifyFactCheckLoaded() {
        try {
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.factCheckLoaded)
        } catch (_: Throwable) {
        }
    }
}
