package org.telegram.messenger.feature.timezones.data.repository

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withTimeoutOrNull
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.timezones.data.mapper.TimezoneMapper
import org.telegram.messenger.feature.timezones.domain.model.TimezoneModel
import org.telegram.messenger.feature.timezones.domain.repository.TimezonesRepository
import org.telegram.ui.Business.TimezonesController

class LegacyTimezonesRepository(
    private val currentAccount: Int
) : TimezonesRepository {

    private val controller: TimezonesController
        get() = TimezonesController.getInstance(currentAccount)

    private val notificationCenter: NotificationCenter
        get() = NotificationCenter.getInstance(currentAccount)

    override fun observeTimezones(): Flow<List<TimezoneModel>> = callbackFlow {
        val observer = NotificationCenter.NotificationCenterDelegate { id, account, _ ->
            if (id == NotificationCenter.timezonesUpdated && account == currentAccount) {
                trySend(TimezoneMapper.toDomainList(controller.getTimezones()))
            }
        }

        notificationCenter.addObserver(observer, NotificationCenter.timezonesUpdated)

        // Initial emission
        val initialList = controller.getTimezones()
        if (initialList.isNotEmpty()) {
            trySend(TimezoneMapper.toDomainList(initialList))
        } else {
            AndroidUtilities.runOnUIThread {
                controller.load()
            }
        }

        awaitClose {
            notificationCenter.removeObserver(observer, NotificationCenter.timezonesUpdated)
        }
    }

    override suspend fun getTimezones(): List<TimezoneModel> {
        val cached = controller.getTimezones()
        if (cached.isNotEmpty()) {
            return TimezoneMapper.toDomainList(cached)
        }
        loadTimezones(false)
        return TimezoneMapper.toDomainList(controller.getTimezones())
    }

    override suspend fun loadTimezones(forceReload: Boolean): Result<List<TimezoneModel>> {
        val cached = controller.getTimezones()
        if (!forceReload && cached.isNotEmpty()) {
            return Result.Success(TimezoneMapper.toDomainList(cached))
        }

        return withTimeoutOrNull(10000L) {
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                val observer = object : NotificationCenter.NotificationCenterDelegate {
                    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
                        if (id == NotificationCenter.timezonesUpdated && account == currentAccount) {
                            notificationCenter.removeObserver(this, NotificationCenter.timezonesUpdated)
                            if (continuation.isActive) {
                                continuation.resumeWith(
                                    kotlin.Result.success(
                                        Result.Success(TimezoneMapper.toDomainList(controller.getTimezones()))
                                    )
                                )
                            }
                        }
                    }
                }

                notificationCenter.addObserver(observer, NotificationCenter.timezonesUpdated)
                continuation.invokeOnCancellation {
                    notificationCenter.removeObserver(observer, NotificationCenter.timezonesUpdated)
                }

                AndroidUtilities.runOnUIThread {
                    controller.load()
                }
            }
        } ?: Result.Success(TimezoneMapper.toDomainList(controller.getTimezones()))
    }

    override fun findTimezone(id: String): TimezoneModel? {
        val tl = controller.findTimezone(id)
        return TimezoneMapper.toDomain(tl)
    }

    override fun getSystemTimezoneId(): String {
        return controller.systemTimezoneId ?: ""
    }

    override fun getTimezoneName(id: String, withOffset: Boolean): String {
        return controller.getTimezoneName(id, withOffset) ?: ""
    }
}
