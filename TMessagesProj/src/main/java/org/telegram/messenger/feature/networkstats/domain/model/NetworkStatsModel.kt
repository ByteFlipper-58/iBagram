package org.telegram.messenger.feature.networkstats.domain.model

enum class NetworkType(val id: Int) {
    MOBILE(0),
    WIFI(1),
    ROAMING(2);

    companion object {
        fun fromId(id: Int): NetworkType = values().firstOrNull { it.id == id } ?: MOBILE
    }
}

enum class TrafficCategory(val id: Int) {
    CALLS(0),
    MESSAGES(1),
    VIDEOS(2),
    AUDIOS(3),
    PHOTOS(4),
    FILES(5),
    TOTAL(6),
    MUSIC(7);

    companion object {
        fun fromId(id: Int): TrafficCategory = values().firstOrNull { it.id == id } ?: TOTAL
    }
}

data class TrafficItemModel(
    val category: TrafficCategory,
    val sentBytes: Long = 0L,
    val receivedBytes: Long = 0L,
    val sentItems: Int = 0,
    val receivedItems: Int = 0
) {
    val totalBytes: Long
        get() = sentBytes + receivedBytes

    val totalItems: Int
        get() = sentItems + receivedItems
}

data class NetworkStatsSummaryModel(
    val networkType: NetworkType,
    val items: Map<TrafficCategory, TrafficItemModel> = emptyMap(),
    val callsTotalTimeSec: Int = 0,
    val resetStatsDateMs: Long = 0L
) {
    fun getItem(category: TrafficCategory): TrafficItemModel {
        return items[category] ?: TrafficItemModel(category = category)
    }

    val totalSentBytes: Long
        get() = items[TrafficCategory.TOTAL]?.sentBytes ?: 0L

    val totalReceivedBytes: Long
        get() = items[TrafficCategory.TOTAL]?.receivedBytes ?: 0L

    val totalBytes: Long
        get() = totalSentBytes + totalReceivedBytes
}

data class NetworkStatsState(
    val selectedNetworkType: NetworkType = NetworkType.MOBILE,
    val statsByNetwork: Map<NetworkType, NetworkStatsSummaryModel> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val currentSummary: NetworkStatsSummaryModel?
        get() = statsByNetwork[selectedNetworkType]
}
