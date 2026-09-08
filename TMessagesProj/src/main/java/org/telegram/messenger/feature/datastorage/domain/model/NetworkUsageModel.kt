package org.telegram.messenger.feature.datastorage.domain.model

data class NetworkUsageModel(
    val networkType: NetworkUsageType,
    val bytesSent: Long = 0L,
    val bytesReceived: Long = 0L,
    val messagesSentBytes: Long = 0L,
    val messagesReceivedBytes: Long = 0L,
    val photosSentBytes: Long = 0L,
    val photosReceivedBytes: Long = 0L,
    val videosSentBytes: Long = 0L,
    val videosReceivedBytes: Long = 0L,
    val audioSentBytes: Long = 0L,
    val audioReceivedBytes: Long = 0L,
    val filesSentBytes: Long = 0L,
    val filesReceivedBytes: Long = 0L,
    val callsSentBytes: Long = 0L,
    val callsReceivedBytes: Long = 0L,
    val callsTotalTimeSeconds: Int = 0,
    val resetDate: Long = 0L
) {
    val totalBytes: Long get() = bytesSent + bytesReceived
}
