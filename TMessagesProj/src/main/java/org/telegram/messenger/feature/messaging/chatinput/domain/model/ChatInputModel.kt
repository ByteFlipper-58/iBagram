package org.telegram.messenger.feature.messaging.chatinput.domain.model

/**
 * Type of media to record in the chat enter view.
 */
enum class RecordType {
    VOICE,
    ROUND_VIDEO
}

/**
 * Current lifecycle status of voice/video note recording.
 */
enum class RecordStatus {
    IDLE,
    RECORDING,
    LOCKED,
    PAUSED,
    PREVIEW
}

/**
 * Virtual panel currently open above or instead of keyboard.
 */
enum class EnterViewPanelMode {
    NONE,
    KEYBOARD,
    EMOJI_STICKER,
    BOT_KEYBOARD,
    ATTACH_ALERT
}

/**
 * Supported rich text formatting styles.
 */
enum class TextFormatStyle {
    BOLD,
    ITALIC,
    MONO,
    STRIKETHROUGH,
    UNDERLINE,
    SPOILER,
    QUOTE
}

/**
 * State representing an active message reply or edit banner.
 */
data class ChatInputReplyQuote(
    val messageId: Long = 0L,
    val isEdit: Boolean = false,
    val text: String? = null,
    val quote: String? = null,
    val quoteOffset: Int = 0
)

/**
 * Send options configuration for the pending message.
 */
data class ChatInputSendOptions(
    val notify: Boolean = true,
    val scheduleDate: Int = 0,
    val scheduleRepeatPeriod: Int = 0,
    val payStars: Long = 0L,
    val effectId: Long = 0L,
    val ttlSeconds: Int = 0,
    val sendAsPeerId: Long = 0L,
    val sendAsFile: Boolean = false
) {
    val isScheduled: Boolean
        get() = scheduleDate > 0

    val isPaid: Boolean
        get() = payStars > 0L
}

/**
 * Voice and round-video recording state.
 */
data class ChatInputRecordState(
    val type: RecordType = RecordType.VOICE,
    val status: RecordStatus = RecordStatus.IDLE,
    val durationMs: Long = 0L,
    val amplitude: Float = 0.0f,
    val isVoiceOnce: Boolean = false
) {
    val isRecordingOrLocked: Boolean
        get() = status == RecordStatus.RECORDING || status == RecordStatus.LOCKED
}

/**
 * Full domain state of the ChatActivityEnterView input bar.
 */
data class ChatInputState(
    val text: String = "",
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val panelMode: EnterViewPanelMode = EnterViewPanelMode.NONE,
    val replyQuote: ChatInputReplyQuote? = null,
    val sendOptions: ChatInputSendOptions = ChatInputSendOptions(),
    val recordState: ChatInputRecordState = ChatInputRecordState(),
    val isAttachButtonVisible: Boolean = true,
    val canSend: Boolean = false
) {
    val hasText: Boolean
        get() = text.isNotBlank()

    val isReplyingOrEditing: Boolean
        get() = replyQuote != null
}
