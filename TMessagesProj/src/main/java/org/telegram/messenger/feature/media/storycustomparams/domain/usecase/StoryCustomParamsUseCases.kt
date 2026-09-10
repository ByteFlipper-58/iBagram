package org.telegram.messenger.feature.media.storycustomparams.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.media.storycustomparams.domain.model.StoryCustomParamsModel
import org.telegram.messenger.feature.media.storycustomparams.domain.model.StoryCustomParamsState
import org.telegram.messenger.feature.media.storycustomparams.domain.model.StoryTranslationParamsModel
import org.telegram.messenger.feature.media.storycustomparams.domain.repository.StoryCustomParamsRepository

/**
 * Checks whether the given story custom parameters or translation details are empty.
 */
class CheckStoryCustomParamsEmptyUseCase {
    operator fun invoke(params: StoryCustomParamsModel?): Boolean {
        return params == null || params.isEmpty()
    }

    operator fun invoke(translation: StoryTranslationParamsModel?): Boolean {
        return translation == null || translation.isEmpty()
    }
}

/**
 * Computes the binary bitmask flags for story parameters according to Telegram's Params_v1 protocol:
 * bit 0 (1): isTranslated
 * bit 1 (2): detectedLng != null
 * bit 2 (4): translatedText != null
 * bit 3 (8): translatedLng != null
 */
class ComputeStoryCustomParamsFlagsUseCase {
    operator fun invoke(translation: StoryTranslationParamsModel): Int {
        return translation.computeFlags()
    }
}

class ObserveStoryCustomParamsStateUseCase(
    private val repository: StoryCustomParamsRepository
) {
    operator fun invoke(): StateFlow<StoryCustomParamsState> = repository.observeState()
}

class GetStoryCustomParamsStateUseCase(
    private val repository: StoryCustomParamsRepository
) {
    operator fun invoke(): StoryCustomParamsState = repository.getState()
}

class GetStoryCustomParamsUseCase(
    private val repository: StoryCustomParamsRepository
) {
    operator fun invoke(dialogId: Long, storyId: Int): StoryCustomParamsModel? =
        repository.getParams(dialogId, storyId)
}

class SaveStoryCustomParamsUseCase(
    private val repository: StoryCustomParamsRepository
) {
    operator fun invoke(params: StoryCustomParamsModel) {
        repository.saveParams(params)
    }
}

class UpdateStoryTranslationUseCase(
    private val repository: StoryCustomParamsRepository
) {
    operator fun invoke(
        dialogId: Long,
        storyId: Int,
        isTranslated: Boolean,
        detectedLang: String?,
        translatedText: String?,
        targetLang: String?
    ) {
        repository.updateTranslation(
            dialogId = dialogId,
            storyId = storyId,
            isTranslated = isTranslated,
            detectedLang = detectedLang,
            translatedText = translatedText,
            targetLang = targetLang
        )
    }
}

class CopyStoryCustomParamsUseCase(
    private val repository: StoryCustomParamsRepository
) {
    operator fun invoke(fromDialogId: Long, fromStoryId: Int, toDialogId: Long, toStoryId: Int) {
        repository.copyParams(fromDialogId, fromStoryId, toDialogId, toStoryId)
    }
}

class RemoveStoryCustomParamsUseCase(
    private val repository: StoryCustomParamsRepository
) {
    operator fun invoke(dialogId: Long, storyId: Int) {
        repository.removeParams(dialogId, storyId)
    }
}

class ClearAllStoryCustomParamsUseCase(
    private val repository: StoryCustomParamsRepository
) {
    operator fun invoke() {
        repository.clearAll()
    }
}
