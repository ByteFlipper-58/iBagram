package org.telegram.messenger.feature.businesslinks.domain.model

data class BusinessLinkModel(
    val link: String,
    val slug: String,
    val title: String? = null,
    val message: String = "",
    val views: Int = 0,
    val hasEntities: Boolean = false
) {
    val displayTitle: String
        get() = if (!title.isNullOrBlank()) title else slug

    val fullUrl: String
        get() = if (link.startsWith("http://") || link.startsWith("https://")) link else "https://$link"
}
