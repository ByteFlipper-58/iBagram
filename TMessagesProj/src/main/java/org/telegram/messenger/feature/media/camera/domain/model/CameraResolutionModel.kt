package org.telegram.messenger.feature.media.camera.domain.model

/**
 * Pure domain model describing a camera resolution.
 */
data class CameraResolutionModel(
    val width: Int,
    val height: Int
) {
    val area: Long
        get() = width.toLong() * height.toLong()

    val aspectRatio: Float
        get() = if (height != 0) width.toFloat() / height.toFloat() else 0f

    fun hasSameAspectRatio(aspectWidth: Int, aspectHeight: Int): Boolean {
        if (aspectWidth == 0 || aspectHeight == 0) return false
        return height == width * aspectHeight / aspectWidth
    }
}
