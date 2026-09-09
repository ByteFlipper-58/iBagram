package org.telegram.messenger.feature.businesslinks.presentation

import org.telegram.messenger.feature.businesslinks.domain.model.BusinessLinkInputModel

sealed interface BusinessLinksEvent {
    data class Load(val forceReload: Boolean = false) : BusinessLinksEvent
    data class CreateLink(val input: BusinessLinkInputModel? = null) : BusinessLinksEvent
    data class EditLink(val slug: String, val title: String?, val message: String) : BusinessLinksEvent
    data class DeleteLink(val slug: String) : BusinessLinksEvent
    object RefreshCanAddNew : BusinessLinksEvent
    object ClearMessages : BusinessLinksEvent
}
