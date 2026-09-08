package org.telegram.messenger.feature.location.domain.model

/**
 * Pure Kotlin domain model representing a geographic location point.
 */
data class GeoPointModel(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f
)
