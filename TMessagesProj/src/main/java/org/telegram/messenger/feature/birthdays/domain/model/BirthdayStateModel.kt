package org.telegram.messenger.feature.birthdays.domain.model

data class BirthdayStateModel(
    val yesterdayKey: String,
    val todayKey: String,
    val tomorrowKey: String,
    val yesterday: List<BirthdayUserModel> = emptyList(),
    val today: List<BirthdayUserModel> = emptyList(),
    val tomorrow: List<BirthdayUserModel> = emptyList()
) {
    val isTodayEmpty: Boolean
        get() = today.isEmpty()

    fun contains(userId: Long): Boolean {
        return yesterday.any { it.id == userId } ||
                today.any { it.id == userId } ||
                tomorrow.any { it.id == userId }
    }
}
