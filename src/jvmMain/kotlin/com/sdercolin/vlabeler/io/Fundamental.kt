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
 * Convert a wave to fundamental.
 */
fun Wave.toFundamental(funConf: AppConf.Fundamental, sampleRate: Float): Fundamental {
    // maybe add other algorithms in the future
    return this.toFundamentalSWIPEPrime(funConf, sampleRate)
}
