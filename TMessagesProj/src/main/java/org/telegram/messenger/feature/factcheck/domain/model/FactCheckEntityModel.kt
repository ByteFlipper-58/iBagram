package org.telegram.messenger.feature.factcheck.domain.model

data class FactCheckEntityModel(
    val offset: Int,
    val length: Int,
    val type: String,
    val url: String? = null
)
