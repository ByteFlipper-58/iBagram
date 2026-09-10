package org.telegram.messenger.feature.business.timezones.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.timezones.domain.model.TimezoneModel

interface TimezonesRepository {
    fun observeTimezones(): Flow<List<TimezoneModel>>
    suspend fun getTimezones(): List<TimezoneModel>
    suspend fun loadTimezones(forceReload: Boolean = false): Result<List<TimezoneModel>>
    fun findTimezone(id: String): TimezoneModel?
    fun getSystemTimezoneId(): String
    fun getTimezoneName(id: String, withOffset: Boolean = false): String
}
