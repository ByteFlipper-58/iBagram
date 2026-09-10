package org.telegram.messenger.feature.messaging.chatinput.data.mapper

import org.telegram.messenger.feature.messaging.chatinput.domain.model.EnterViewPanelMode
import org.telegram.messenger.feature.messaging.chatinput.domain.model.RecordStatus
import org.telegram.messenger.feature.messaging.chatinput.domain.model.RecordType
import java.util.Locale

/**
 * Mapper for ChatInput recording state, panel identifiers, and duration formatters.
 */
object ChatInputMapper {

    fun formatRecordDuration(durationMs: Long): String {
        val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000L).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun mapRecordTypeToInt(type: RecordType): Int {
        return when (type) {
            RecordType.VOICE -> 0
            RecordType.ROUND_VIDEO -> 1
        }
    }

    fun mapIntToRecordType(value: Int): RecordType {
        return if (value == 1) RecordType.ROUND_VIDEO else RecordType.VOICE
    }

    fun getPanelName(mode: EnterViewPanelMode): String {
        return when (mode) {
            EnterViewPanelMode.NONE -> "None"
            EnterViewPanelMode.KEYBOARD -> "Keyboard"
            EnterViewPanelMode.EMOJI_STICKER -> "Emoji & Stickers"
            EnterViewPanelMode.BOT_KEYBOARD -> "Bot Keyboard"
            EnterViewPanelMode.ATTACH_ALERT -> "Attach Sheet"
        }
    }

    fun getRecordStatusName(status: RecordStatus): String {
        return when (status) {
            RecordStatus.IDLE -> "Idle"
            RecordStatus.RECORDING -> "Recording"
            RecordStatus.LOCKED -> "Locked"
            RecordStatus.PAUSED -> "Paused"
            RecordStatus.PREVIEW -> "Preview"
        }
    }
}
