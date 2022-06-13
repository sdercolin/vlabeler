package com.sdercolin.vlabeler.ui.labeler

import androidx.compose.ui.unit.Density

data class CanvasParams(
    val dataLength: Int,
    val resolution: Int,
    val density: Density
) {
    val lengthInPixel = dataLength / resolution
    val canvasWidthInDp = with(density) { lengthInPixel.toDp() }

    companion object {

        fun canIncrease(resolution: Int) = resolution < MaxResolution
        fun canDecrease(resolution: Int) = resolution > MinResolution
        fun increaseFrom(resolution: Int) = resolution.plus(Interval).coerceAtMost(MaxResolution)
        fun decreaseFrom(resolution: Int) = resolution.minus(Interval).coerceAtLeast(MinResolution)

        const val MaxResolution = 400
        const val MinResolution = 10
        private const val Interval = 20
    }
}
