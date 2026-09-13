package org.telegram.messenger.feature.business.timezones.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Business.TimezonesController
import java.time.ZoneId
import java.util.ArrayList

/**
 * Local data source managing cached timezones and bridging to TimezonesController.
 */
open class TimezonesLocalDataSource(
    private val currentAccount: Int
) {
    private val testTimezones = mutableListOf<TLRPC.TL_timezone>()
    private var testSystemTimezoneId: String? = null

    open fun setTestTimezones(list: List<TLRPC.TL_timezone>) {
        testTimezones.clear()
        testTimezones.addAll(list)
    }

    open fun setTestSystemTimezoneId(id: String) {
        testSystemTimezoneId = id
    }

    open fun getTimezones(): List<TLRPC.TL_timezone> {
        if (testTimezones.isNotEmpty()) {
            return ArrayList(testTimezones)
        }
        return try {
            val controller = TimezonesController.getInstance(currentAccount)
            controller.timezones ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun saveTimezones(list: List<TLRPC.TL_timezone>) {
        testTimezones.clear()
        testTimezones.addAll(list)
    }

    open fun findTimezone(id: String): TLRPC.TL_timezone? {
        testTimezones.find { it.id == id }?.let { return it }
        return try {
            TimezonesController.getInstance(currentAccount).findTimezone(id)
        } catch (_: Throwable) {
            null
        }
    }

    open fun getSystemTimezoneId(): String {
        testSystemTimezoneId?.let { return it }
        return try {
            TimezonesController.getInstance(currentAccount).systemTimezoneId ?: ZoneId.systemDefault().id
        } catch (_: Throwable) {
            "UTC"
        }
    }

    open fun getTimezoneName(id: String, withOffset: Boolean): String {
        val found = findTimezone(id)
        if (found != null) {
            return if (withOffset) {
                "${found.name}, ${formatOffset(found.utc_offset)}"
            } else {
                found.name ?: id
            }
        }
        return try {
            TimezonesController.getInstance(currentAccount).getTimezoneName(id, withOffset) ?: id
        } catch (_: Throwable) {
            id
        }
    }

    open fun observeTimezonesUpdated(): Flow<Unit> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.timezonesUpdated).map { }
    }

    private fun formatOffset(utcOffset: Int): String {
        if (utcOffset == 0) return "GMT"
        val sign = if (utcOffset < 0) "-" else "+"
        val totalMinutes = Math.abs(utcOffset) / 60
        val hr = totalMinutes / 60
        val min = totalMinutes % 60
        val hrStr = if (hr < 10) "0$hr" else hr.toString()
        val minStr = if (min < 10) "0$min" else min.toString()
        return "GMT$sign$hrStr:$minStr"
    }
}
