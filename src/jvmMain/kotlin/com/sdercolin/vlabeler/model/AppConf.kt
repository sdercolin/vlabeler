package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Basic configurations of app
 * @param painter Configurations about chart painting
 * @param autoSaveIntervalSecond Interval in second to auto-save the project; never do auto-save if the value is null
 */
@Serializable
@Immutable
data class AppConf(
    val painter: Painter = Painter(),
    val autoSaveIntervalSecond: Int? = 30
) {
    /**
     * Configurations about chart painting
     * @param canvasResolution Configurations about the canvas's resolution
     * @param maxDataChunkSize Max number of sample points in one chunk drawn in the painter
     * @param amplitude Configurations about amplitude (waveforms) painting
     * @param spectrogram Configurations about spectrogram painting
     */
    @Serializable
    @Immutable
    data class Painter(
        val canvasResolution: CanvasResolution = CanvasResolution(),
        val maxDataChunkSize: Int = 441000,
        val amplitude: Amplitude = Amplitude(),
        val spectrogram: Spectrogram = Spectrogram()
    )

    /**
     * Configurations about the canvas's resolution.
     * This resolution is defined as number of sample points included in 1 pixel.
     * @param default Default value used when editor is launched
     * @param min Minimum value of the resolution
     * @param max Maximum value of the resolution
     * @param step Linear step length when resolution is changed by "+" "-" buttons
     */
    @Serializable
    @Immutable
    data class CanvasResolution(
        val default: Int = 100,
        val min: Int = 10,
        val max: Int = 400,
        val step: Int = 20
    )

    /**
     * Configurations about amplitude(waveforms) painting
     * @param unitSize Size of the unit used when drawing the waveforms
     * @param intensityAccuracy Height of the container bitmap in pixel
     * @param yAxisBlankRate Height rate of the extra blank region displayed in both top and bottom to
     * the height of the waveform
     */
    @Serializable
    @Immutable
    data class Amplitude(
        val unitSize: Int = 40,
        val intensityAccuracy: Int = 1000,
        val yAxisBlankRate: Float = 0.1f
    )

    /**
     * Configurations about spectrogram painting
     * @param enabled True if spectrogram is calculated and shown
     * @param heightWeight Height weight of the spectrogram to the amplitude form (whose weight is 1)
     * @param frameSize Number of samples that each FFT-frame should have
     * @param maxFrequency Max frequency (Hz) displayed
     * @param minIntensity Min intensity (dB) displayed in the heatmap
     * @param maxIntensity Max intensity (dB) displayed in the heatmap
     * @param windowType Window type used in the Short-Time FT. See [WindowType] for options
     */
    @Serializable
    @Immutable
    data class Spectrogram(
        val enabled: Boolean = false,
        val heightWeight: Float = 0.75f,
        val frameSize: Int = 300,
        val maxFrequency: Int = 15000,
        val minIntensity: Int = 0,
        val maxIntensity: Int = 45,
        val windowType: WindowType = WindowType.Hamming
    )

    @Serializable
    @Immutable
    enum class WindowType {
        Hamming,
        Hanning,
        Rectangular,
        Triangular,
        Blackman,
        Bartlett
    }
}
