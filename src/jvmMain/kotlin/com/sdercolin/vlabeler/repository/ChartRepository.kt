package com.sdercolin.vlabeler.repository

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap

@Stable
object ChartRepository {

    private val waveforms = mutableMapOf<Pair<Int, Int>, ImageBitmap>()
    private val spectrograms = mutableMapOf<Int, ImageBitmap>()

    fun getWaveform(channelIndex: Int, chunkIndex: Int) = waveforms[channelIndex to chunkIndex]
    fun getSpectrogram(chunkIndex: Int) = spectrograms[chunkIndex]

    fun putWaveform(channelIndex: Int, chunkIndex: Int, waveform: ImageBitmap) {
        waveforms[channelIndex to chunkIndex] = waveform
    }

    fun putSpectrogram(chunkIndex: Int, spectrogram: ImageBitmap) {
        spectrograms[chunkIndex] = spectrogram
    }

    fun clear() {
        waveforms.clear()
        spectrograms.clear()
    }
}
