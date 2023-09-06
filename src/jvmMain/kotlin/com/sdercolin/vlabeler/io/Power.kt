package com.sdercolin.vlabeler.io

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.AppConf
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Data class to represent a power.
 *
 * @param data A list of power data. Each element is a power data of a channel.
 */
@Immutable
data class Power(val data: List<FloatArray>)

/**
 * Convert a wave to power.
 *
 * @param conf The power configuration.
 */
fun Wave.toPower(conf: AppConf.Power): Power {
    val ref = 2.0.pow(NORMALIZED_SAMPLE_SIZE_IN_BITS - 1).toFloat()
    // merge channels
    val merged = if (conf.mergeChannels) {
        val dataLength = channels.minOf { it.data.size }
        val data = Wave.Channel(
            FloatArray(dataLength) { i ->
                channels.sumOf { it.data[i].toDouble() }.toFloat() / channels.size
            },
        )
        listOf(data)
    } else {
        channels
    }
    // calculate power
    val power = merged.map { channel ->
        val padding = (conf.windowSize - conf.unitSize) / 2
        val wave = if (padding > 0) List(padding) { 0.0f } + channel.data.toList() else channel.data.toList()
        wave.windowed(conf.windowSize, conf.unitSize, partialWindows = true)
            .map {
                // RMS
                sqrt(it.sumOf { value -> (value * value).toDouble() } / it.size)
            }
            .map { rms ->
                // dB
                (20 * log10(maxOf(rms, 1.0) / ref)).toFloat()
            }
            .toFloatArray()
    }
    return Power(power)
}
