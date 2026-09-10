package org.telegram.messenger.feature.networkstats.presentation

import org.telegram.messenger.feature.networkstats.domain.model.NetworkType
import org.telegram.messenger.feature.networkstats.domain.model.TrafficCategory

data class FormattedTrafficItem(
    val category: TrafficCategory,
    val sentBytesFormatted: String,
    val receivedBytesFormatted: String,
    val totalBytesFormatted: String,
    val sentItemsCount: Int,
    val receivedItemsCount: Int,
    val totalItemsCount: Int
)

data class FormattedNetworkStatsSummary(
    val networkType: NetworkType,
    val items: Map<TrafficCategory, FormattedTrafficItem>,
    val callsDurationFormatted: String,
    val resetStatsDateMs: Long,
    val totalSentFormatted: String,
    val totalReceivedFormatted: String,
    val grandTotalFormatted: String
)

data class NetworkStatsUiState(
    val selectedNetworkType: NetworkType = NetworkType.MOBILE,
    val currentSummary: FormattedNetworkStatsSummary? = null,
    val allSummaries: Map<NetworkType, FormattedNetworkStatsSummary> = emptyMap(),
    val isLoading: Boolean = false,
    val infoMessage: String? = null,
    val error: String? = null
)
