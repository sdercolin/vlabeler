package com.sdercolin.vlabeler.io

import javax.sound.sampled.AudioFormat

fun AudioFormat.normalize(maxSampleRate: Int): AudioFormat {
    val sampleRate = if (maxSampleRate == 0) {
        this.sampleRate
    } else {
        this.sampleRate.coerceAtMost(maxSampleRate.toFloat())
    }
    return AudioFormat(
        sampleRate,
        NormalizedSampleSizeInBits,
        channels,
        true,
        isBigEndian,
    )
}

const val NormalizedSampleSizeInBits = 16
