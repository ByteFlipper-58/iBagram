package org.telegram.messenger.feature.business.businesslinks.domain.model

data class BusinessLinksStateModel(
    val links: List<BusinessLinkModel> = emptyList(),
    val maxLimit: Int = 0,
    val isLoading: Boolean = false
) {
    val canAddNew: Boolean
        get() = links.size < maxLimit
}
