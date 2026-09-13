package org.telegram.messenger.feature.media.storycustomparams.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.media.storycustomparams.data.datasource.StoryCustomParamsLocalDataSource
import org.telegram.messenger.feature.media.storycustomparams.data.datasource.StoryCustomParamsRemoteDataSource
import org.telegram.messenger.feature.media.storycustomparams.domain.model.StoryCustomParamsModel
import org.telegram.messenger.feature.media.storycustomparams.domain.model.StoryCustomParamsState
import org.telegram.messenger.feature.media.storycustomparams.domain.repository.StoryCustomParamsRepository

/**
 * Production implementation of StoryCustomParamsRepository coordinating local and remote data sources.
 */
class StoryCustomParamsRepositoryImpl(
    private val account: Int,
    private val localDataSource: StoryCustomParamsLocalDataSource,
    private val remoteDataSource: StoryCustomParamsRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : StoryCustomParamsRepository {

    private val _state = MutableStateFlow(
        StoryCustomParamsState(paramsByStoryKey = localDataSource.getAllParams())
    )

    override fun observeState(): StateFlow<StoryCustomParamsState> = _state.asStateFlow()

    override fun getState(): StoryCustomParamsState = _state.value

    override fun getParams(dialogId: Long, storyId: Int): StoryCustomParamsModel? {
        return localDataSource.getParams(dialogId, storyId)
    }

    override fun saveParams(params: StoryCustomParamsModel) {
        localDataSource.saveParams(params)
        syncState()
    }

    override fun updateTranslation(
        dialogId: Long,
        storyId: Int,
        isTranslated: Boolean,
        detectedLang: String?,
        translatedText: String?,
        targetLang: String?
    ) {
        localDataSource.updateTranslation(
            dialogId = dialogId,
            storyId = storyId,
            isTranslated = isTranslated,
            detectedLang = detectedLang,
            translatedText = translatedText,
            targetLang = targetLang
        )
        syncState()
    }

    override fun copyParams(fromDialogId: Long, fromStoryId: Int, toDialogId: Long, toStoryId: Int) {
        localDataSource.copyParams(fromDialogId, fromStoryId, toDialogId, toStoryId)
        syncState()
    }

    override fun removeParams(dialogId: Long, storyId: Int) {
        localDataSource.removeParams(dialogId, storyId)
        syncState()
    }

    override fun clearAll() {
        localDataSource.clearAll()
        syncState()
    }

    private fun syncState() {
        _state.update { curr ->
            curr.copy(paramsByStoryKey = localDataSource.getAllParams())
        }
    }
}
