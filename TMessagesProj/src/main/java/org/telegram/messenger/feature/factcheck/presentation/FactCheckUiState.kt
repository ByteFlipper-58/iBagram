package org.telegram.messenger.feature.factcheck.presentation

import org.telegram.messenger.feature.factcheck.domain.model.FactCheckEntityModel
import org.telegram.messenger.feature.factcheck.domain.model.FactCheckModel

data class FactCheckUiState(
    val dialogId: Long = 0L,
    val messageId: Int = 0,
    val factCheck: FactCheckModel? = null,
    val characterLimit: Int = 1024,
    val currentInputText: String = "",
    val currentEntities: List<FactCheckEntityModel> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val actionSuccessMessage: String? = null
) {
    val remainingCharacters: Int
        get() = characterLimit - currentInputText.length

    val isOverLimit: Boolean
        get() = remainingCharacters < 0

    val hasExistingFactCheck: Boolean
        get() = factCheck != null && !factCheck.text.isNullOrBlank()
}
