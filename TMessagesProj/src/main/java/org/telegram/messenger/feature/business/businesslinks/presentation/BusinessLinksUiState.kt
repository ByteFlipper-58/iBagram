package org.telegram.messenger.feature.business.businesslinks.presentation

import org.telegram.messenger.feature.business.businesslinks.domain.model.BusinessLinkModel

data class BusinessLinksUiState(
    val links: List<BusinessLinkModel> = emptyList(),
    val canAddNew: Boolean = true,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)
