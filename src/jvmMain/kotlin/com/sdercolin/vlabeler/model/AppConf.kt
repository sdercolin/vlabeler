package com.sdercolin.vlabeler.model

import kotlinx.serialization.Serializable

/**
 * Basic configurations of app
 */
@Serializable
data class AppConf(
    val painter: Painter = Painter()
) {
    @Serializable
    data class Painter(
        /**
         * Number of sample points actually drawn into 1 pixel.
         */
        val dataDensity: Int = 10,
        /**
         * Number of sample points included in 1 pixel.
         * The minimum value should not be smaller than [dataDensity]
         */
        val canvasResolution: CanvasResolution = CanvasResolution()
    )

    @Serializable
    data class CanvasResolution(
        val default: Int = 100,
        val min: Int = 10,
        val max: Int = 400,
        val step: Int = 20
    )
}