package org.telegram.messenger.feature.factcheck.domain.model

data class FactCheckModel(
    val hash: Long,
    val dialogId: Long,
    val messageId: Int,
    val text: String,
    val entities: List<FactCheckEntityModel> = emptyList(),
    val country: String? = null,
    val needCheck: Boolean = false
) {
    val isEmpty: Boolean get() = text.isBlank()
}
