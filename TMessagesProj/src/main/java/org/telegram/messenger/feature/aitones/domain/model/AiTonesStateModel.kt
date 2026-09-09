package org.telegram.messenger.feature.aitones.domain.model

/**
 * Domain representation of AI Compose tones state.
 */
data class AiTonesStateModel(
    val tones: List<AiToneModel> = emptyList(),
    val savedCount: Int = 0,
    val isLoading: Boolean = false,
    val hash: Long = 0L,
)
