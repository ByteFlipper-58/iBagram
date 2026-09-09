package org.telegram.messenger.feature.fileref.presentation

import org.telegram.messenger.feature.fileref.domain.model.FileRefStatsModel

/**
 * UI State describing in-flight file reference renewals, queues, and statistics.
 */
data class FileRefUiState(
    val isLoading: Boolean = false,
    val stats: FileRefStatsModel = FileRefStatsModel(),
    val infoMessage: String? = null
) {
    val hasActiveRequests: Boolean
        get() = stats.hasActiveRequests

    val totalRenewedCount: Long
        get() = stats.totalRenewedCount

    val cachedResponsesCount: Int
        get() = stats.cachedResponsesCount
}
