package org.telegram.messenger.feature.messaging.aitones.data.datasource

import java.util.Base64
import org.telegram.messenger.MessagesController
import org.telegram.tgnet.SerializedData
import org.telegram.tgnet.tl.TL_aicompose
import java.util.ArrayList

/**
 * Local data source for caching and persistence of AI Compose tones.
 */
class AiTonesLocalDataSource(
    private val currentAccount: Int
) {

    private val inMemoryTones = ArrayList<TL_aicompose.AiComposeTone>()
    private var inMemoryHash: Long = 0L

    fun getLocalTones(): Pair<Long, List<TL_aicompose.AiComposeTone>> {
        try {
            val mainSettings = MessagesController.getInstance(currentAccount)?.mainSettings
            val base64 = mainSettings?.getString("ai_styles", null)
            if (base64 != null) {
                val bytes = Base64.getDecoder().decode(base64)
                val data = SerializedData(bytes)
                val tonesObj = TL_aicompose.Tones.TLdeserialize(data, data.readInt32(true), true)
                if (tonesObj is TL_aicompose.TL_tones) {
                    inMemoryHash = tonesObj.hash
                    inMemoryTones.clear()
                    inMemoryTones.addAll(tonesObj.tones)
                }
            }
        } catch (_: Throwable) {
            // Headless / fallback handling
        }
        return Pair(inMemoryHash, inMemoryTones.toList())
    }

    fun saveLocalTones(hash: Long, tones: List<TL_aicompose.AiComposeTone>) {
        inMemoryHash = hash
        if (tones !== inMemoryTones) {
            inMemoryTones.clear()
            inMemoryTones.addAll(tones)
        }

        try {
            val tlTones = TL_aicompose.TL_tones().apply {
                this.hash = hash
                this.tones.addAll(tones)
            }
            val data = SerializedData(tlTones.objectSize)
            tlTones.serializeToStream(data)
            val base64 = Base64.getEncoder().encodeToString(data.toByteArray())
            MessagesController.getInstance(currentAccount)?.mainSettings?.edit()
                ?.putString("ai_styles", base64)
                ?.apply()
        } catch (_: Throwable) {
            // Headless / fallback handling
        }
    }

    fun addTone(tone: TL_aicompose.AiComposeTone) {
        val toneId = if (tone is TL_aicompose.TL_aiComposeTone) tone.id else null
        val exists = toneId != null && inMemoryTones.any {
            it is TL_aicompose.TL_aiComposeTone && it.id == toneId
        }
        if (!exists) {
            inMemoryTones.add(0, tone)
            saveLocalTones(inMemoryHash, inMemoryTones)
        }
    }

    fun removeTone(toneId: Long): Boolean {
        var removed = false
        val iterator = inMemoryTones.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item is TL_aicompose.TL_aiComposeTone && item.id == toneId) {
                iterator.remove()
                removed = true
                break
            }
        }
        if (removed) {
            saveLocalTones(inMemoryHash, inMemoryTones)
        }
        return removed
    }

    fun editTone(updatedTone: TL_aicompose.TL_aiComposeTone): Boolean {
        var edited = false
        for (i in 0 until inMemoryTones.size) {
            val item = inMemoryTones[i]
            if (item is TL_aicompose.TL_aiComposeTone && item.id == updatedTone.id) {
                inMemoryTones[i] = updatedTone
                edited = true
                break
            }
        }
        if (edited) {
            saveLocalTones(inMemoryHash, inMemoryTones)
        }
        return edited
    }

    fun getCurrentTones(): List<TL_aicompose.AiComposeTone> {
        return inMemoryTones.toList()
    }

    fun getCurrentHash(): Long = inMemoryHash

    fun setInMemoryTones(hash: Long, tones: List<TL_aicompose.AiComposeTone>) {
        inMemoryHash = hash
        if (tones !== inMemoryTones) {
            inMemoryTones.clear()
            inMemoryTones.addAll(tones)
        }
    }
}
