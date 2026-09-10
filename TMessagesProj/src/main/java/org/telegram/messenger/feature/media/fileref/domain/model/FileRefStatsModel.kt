package org.telegram.messenger.feature.media.fileref.domain.model

/**
 * Domain model describing aggregated statistics and queues of FileRefController.
 */
data class FileRefStatsModel(
    val activeRequestsCount: Int = 0,
    val cachedResponsesCount: Int = 0,
    val pendingLocationsCount: Int = 0,
    val totalRenewedCount: Long = 0L,
    val lastRenewalTime: Long = 0L
) {
    val hasActiveRequests: Boolean
        get() = activeRequestsCount > 0 || pendingLocationsCount > 0
}
