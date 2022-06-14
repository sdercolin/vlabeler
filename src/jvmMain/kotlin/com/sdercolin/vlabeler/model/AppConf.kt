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
         * Number of sample points included in 1 pixel.
         * The minimum value should not be smaller than [dataDensity]
         */
        val canvasResolution: CanvasResolution = CanvasResolution(),
        /**
         * Configurations of the wave amplitude (waveform) canvas
         */
        val amplitude: Amplitude = Amplitude(),
        /**
         * Configurations of the spectrogram canvas
         */
        val spectrogram: Spectrogram = Spectrogram()
    )

    @Serializable
    data class CanvasResolution(
        val default: Int = 100,
        val min: Int = 10,
        val max: Int = 400,
        val step: Int = 20
    )

    @Serializable
    data class Amplitude(
        /**
         * Number of sample points actually drawn into 1 pixel.
         */
        val dataDensity: Int = 10,
        /**
         * Height rate of the extra blank region displayed in both top and bottom to the height of the waveform
         */
        val yAxisBlankRate: Float = 0.1f
    )

    @Serializable
    data class Spectrogram(
        val enabled: Boolean = false,
        /**
         * Height weight of the spectrogram to the amplitude form
         */
        val heightWeight: Float = 0.75f,
        /**
         * Number of samples that each FFT-frame should have
         */
        val frameSize: Int = 512,
        /**
         * Max frequency (Hz) displayed
         */
        val maxFrequency: Int = 15000,
        /**
         * Min intensity (dB) displayed in the heatmap
         */
        val minIntensity: Int = -20,
        /**
         * Max intensity (dB) displayed in the heatmap
         */
        val maxIntensity: Int = 40,
        /**
         * Window type used in the Short-Time FT
         */
        val windowType: WindowType = WindowType.Hamming
    )

    @Serializable
    enum class WindowType {
        Hamming,
        Hanning,
        Rectangular,
        Triangular,
        Blackman,
        Bartlett
    }
}