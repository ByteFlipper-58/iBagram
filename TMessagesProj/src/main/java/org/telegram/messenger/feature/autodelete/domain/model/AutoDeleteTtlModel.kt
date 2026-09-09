package org.telegram.messenger.feature.autodelete.domain.model

data class AutoDeleteTtlModel(
    val periodSeconds: Int
) {
    val isEnabled: Boolean get() = periodSeconds > 0
    val periodMinutes: Int get() = periodSeconds / 60
    val periodDays: Int get() = periodSeconds / (24 * 3600)

    companion object {
        val OFF = AutoDeleteTtlModel(0)
        val ONE_DAY = AutoDeleteTtlModel(24 * 60 * 60)
        val ONE_WEEK = AutoDeleteTtlModel(7 * 24 * 60 * 60)
        val ONE_MONTH = AutoDeleteTtlModel(31 * 24 * 60 * 60)

        fun fromMinutes(minutes: Int): AutoDeleteTtlModel = AutoDeleteTtlModel(minutes * 60)
        fun fromDays(days: Int): AutoDeleteTtlModel = AutoDeleteTtlModel(days * 24 * 60 * 60)
    }
}
