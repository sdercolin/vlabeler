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
    val ref = 2.0.pow(NormalizedSampleSizeInBits - 1).toFloat()
    val power = channels.map { channel ->
        channel.data.toList()
            .chunked(conf.unitSize)
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
