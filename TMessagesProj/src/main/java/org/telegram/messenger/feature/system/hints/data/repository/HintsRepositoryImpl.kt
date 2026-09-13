package org.telegram.messenger.feature.system.hints.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.system.hints.data.datasource.HintsLocalDataSource
import org.telegram.messenger.feature.system.hints.data.datasource.HintsRemoteDataSource
import org.telegram.messenger.feature.system.hints.data.mapper.HintMapper
import org.telegram.messenger.feature.system.hints.domain.model.HintModel
import org.telegram.messenger.feature.system.hints.domain.model.HintType
import org.telegram.messenger.feature.system.hints.domain.model.HintsStateModel
import org.telegram.messenger.feature.system.hints.domain.repository.HintsRepository

/**
 * Production implementation of HintsRepository coordinating local storage and remote sync.
 */
class HintsRepositoryImpl(
    private val account: Int,
    private val localDataSource: HintsLocalDataSource,
    private val remoteDataSource: HintsRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HintsRepository {

    private val _hintsFlow = MutableStateFlow(readCurrentState())

    override fun observeHints(): Flow<HintsStateModel> = _hintsFlow.asStateFlow()

    override fun getHintsState(): HintsStateModel = _hintsFlow.value

    override fun getHint(type: HintType): HintModel {
        val showsCount = localDataSource.getShowsCount(type)
        return HintMapper.toDomainModel(type, showsCount)
    }

    override fun shouldShowHint(type: HintType): Boolean {
        return localDataSource.shouldShow(type)
    }

    override fun incrementHint(type: HintType) {
        localDataSource.increment(type)
        updateState()
    }

    override fun doNotShowAgain(type: HintType) {
        localDataSource.doNotShowAgain(type)
        updateState()
    }

    override fun resetHint(type: HintType) {
        localDataSource.reset(type)
        updateState()
    }

    override fun resetAllHints() {
        localDataSource.resetAll()
        updateState()
    }

    private fun readCurrentState(): HintsStateModel {
        val map = mutableMapOf<HintType, HintModel>()
        for (type in HintType.entries) {
            val count = localDataSource.getShowsCount(type)
            map[type] = HintMapper.toDomainModel(type, count)
        }
        return HintsStateModel(hints = map)
    }

    private fun updateState() {
        _hintsFlow.value = readCurrentState()
    }
}
