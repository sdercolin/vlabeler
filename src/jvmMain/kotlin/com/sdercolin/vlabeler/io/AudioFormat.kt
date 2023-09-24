package com.sdercolin.vlabeler.io

import javax.sound.sampled.AudioFormat

/**
 * Normalize the sample rate of the audio format to the given maximum sample rate.
 * If the maximum sample rate is 0, the sample rate will not be changed.
 */
fun AudioFormat.normalize(maxSampleRate: Int): AudioFormat {
    val sampleRate = if (maxSampleRate == 0) {
        this.sampleRate
    } else {
        this.sampleRate.coerceAtMost(maxSampleRate.toFloat())
    }
    return AudioFormat(
        sampleRate,
        NORMALIZED_SAMPLE_SIZE_IN_BITS,
        channels,
        true,
        isBigEndian,
    )
}

const val NORMALIZED_SAMPLE_SIZE_IN_BITS = 16
