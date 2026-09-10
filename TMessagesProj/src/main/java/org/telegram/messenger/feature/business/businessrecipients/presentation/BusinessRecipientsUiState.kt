package org.telegram.messenger.feature.business.businessrecipients.presentation

import org.telegram.messenger.feature.business.businessrecipients.domain.model.BusinessRecipientsModel
import org.telegram.messenger.feature.business.businessrecipients.domain.model.RecipientValidationResult

data class BusinessRecipientsUiState(
    val initialModel: BusinessRecipientsModel = BusinessRecipientsModel(),
    val currentModel: BusinessRecipientsModel = BusinessRecipientsModel(),
    val hasChanges: Boolean = false,
    val validationResult: RecipientValidationResult = RecipientValidationResult(isValid = true)
)
