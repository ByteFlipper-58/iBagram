package org.telegram.messenger.feature.business.timezones.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.timezones.data.datasource.TimezonesLocalDataSource
import org.telegram.messenger.feature.business.timezones.data.datasource.TimezonesRemoteDataSource
import org.telegram.messenger.feature.business.timezones.data.mapper.TimezoneMapper
import org.telegram.messenger.feature.business.timezones.domain.model.TimezoneModel
import org.telegram.messenger.feature.business.timezones.domain.repository.TimezonesRepository

/**
 * Modern repository implementation managing business timezones with local caching and MTProto RPC syncing.
 */
class TimezonesRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: TimezonesLocalDataSource,
    private val remoteDataSource: TimezonesRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : TimezonesRepository {

    override fun observeTimezones(): Flow<List<TimezoneModel>> {
        return localDataSource.observeTimezonesUpdated()
            .map { getTimezones() }
            .onStart { emit(getTimezones()) }
            .flowOn(mainDispatcher)
    }

    override suspend fun getTimezones(): List<TimezoneModel> = withContext(mainDispatcher) {
        val cached = localDataSource.getTimezones()
        if (cached.isNotEmpty()) {
            return@withContext TimezoneMapper.toDomainList(cached)
        }
        val remote = remoteDataSource.loadTimezones()
        if (remote is Result.Success && remote.data.isNotEmpty()) {
            localDataSource.saveTimezones(remote.data)
            TimezoneMapper.toDomainList(remote.data)
        } else {
            TimezoneMapper.toDomainList(cached)
        }
    }

    override suspend fun loadTimezones(forceReload: Boolean): Result<List<TimezoneModel>> = withContext(mainDispatcher) {
        val cached = localDataSource.getTimezones()
        if (!forceReload && cached.isNotEmpty()) {
            return@withContext Result.success(TimezoneMapper.toDomainList(cached))
        }
        val remote = remoteDataSource.loadTimezones()
        remote.map { list ->
            if (list.isNotEmpty()) {
                localDataSource.saveTimezones(list)
            }
            TimezoneMapper.toDomainList(list)
        }
    }

    override fun findTimezone(id: String): TimezoneModel? {
        val found = localDataSource.findTimezone(id)
        return TimezoneMapper.toDomain(found)
    }

    override fun getSystemTimezoneId(): String {
        return localDataSource.getSystemTimezoneId()
    }

    override fun getTimezoneName(id: String, withOffset: Boolean): String {
        return localDataSource.getTimezoneName(id, withOffset)
    }
}
