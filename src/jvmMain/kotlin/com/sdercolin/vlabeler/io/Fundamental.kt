package com.sdercolin.vlabeler.io

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.AppConf
import kotlin.math.log2
import kotlin.math.pow

/**
 * Data class to represent a fundamental.
 *
 * @param freq A list of fundamental data.
 * @param corr A list of correlation data.
 */
@Immutable
data class Fundamental(
    val freq: List<Float>,
    val corr: List<Float>,
)

/**
 * Convert a wave to fundamental.
 */
fun Wave.toFundamental(funConf: AppConf.Fundamental, sampleRate: Float): Fundamental {
    // maybe add other algorithms in the future
    return this.toFundamentalSwipePrime(funConf, sampleRate)
}

/**
 * Convert frequency to semitone.
 */
object Semitone {
    fun fromFrequency(freq: Float): Float {
        return 12f * (log2(freq / 440f)) + 69f
    }

    fun toFrequency(semitone: Float): Float {
        return 440f * 2f.pow((semitone - 69f) / 12f)
    }
}
