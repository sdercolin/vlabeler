package com.sdercolin.vlabeler.io

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.AppConf

/**
 * Data class to represent a fundamental.
 *
 * @param data A list of fundamental data. Each element is a fundamental data.
 */
@Immutable
data class Fundamental(val data: List<Float>)

/**
 * Convert a spectrogram to fundamental.
 */
fun Spectrogram.toFundamental(funConf: AppConf.Fundamental, sampleRate: Float): Fundamental {
    val maxFrequency = sampleRate / 2
    val data = this.data.map { frame ->
        val maxIndex = frame.withIndex().maxByOrNull { it.value }?.index ?: 0
        val fundamental = maxIndex.toFloat() * maxFrequency / frame.size
        fundamental
    }
    return Fundamental(data)
}
