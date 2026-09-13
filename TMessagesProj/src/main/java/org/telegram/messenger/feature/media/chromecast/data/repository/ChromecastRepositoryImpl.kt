package org.telegram.messenger.feature.media.chromecast.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.telegram.messenger.feature.media.chromecast.data.datasource.ChromecastLocalDataSource
import org.telegram.messenger.feature.media.chromecast.data.datasource.ChromecastRemoteDataSource
import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastMediaModel
import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastStateModel
import org.telegram.messenger.feature.media.chromecast.domain.repository.ChromecastRepository
import java.io.File

/**
 * Modern implementation of [ChromecastRepository] coordinating local Cast state
 * and remote Cast session operations.
 */
class ChromecastRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: ChromecastLocalDataSource,
    private val remoteDataSource: ChromecastRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ChromecastRepository {

    override fun observeChromecastState(): Flow<ChromecastStateModel> =
        localDataSource.observeState()

    override fun getChromecastState(): ChromecastStateModel {
        val casting = remoteDataSource.isCasting()
        localDataSource.updateCastingState(casting)
        return localDataSource.getState()
    }

    override fun isCasting(): Boolean =
        remoteDataSource.isCasting()

    override fun isPlaying(media: ChromecastMediaModel): Boolean =
        isCasting() && localDataSource.getActiveMedia() == media

    override suspend fun castMedia(media: ChromecastMediaModel): Result<Unit> = withContext(mainDispatcher) {
        runCatching {
            val isCasting = remoteDataSource.isCasting()
            localDataSource.setActiveMedia(media, isCasting = true)
        }
    }

    override suspend fun stopCasting(): Result<Unit> = withContext(mainDispatcher) {
        runCatching {
            remoteDataSource.stopCasting()
            localDataSource.setActiveMedia(null, isCasting = false)
        }
    }

    override suspend fun setCoverFile(file: File?): Result<String?> = withContext(mainDispatcher) {
        val result = remoteDataSource.setCoverFile(file)
        if (result.isSuccess) {
            localDataSource.setCoverPath(result.getOrNull())
        }
        result
    }
}
