package com.sdercolin.vlabeler.io

import javax.sound.sampled.AudioFormat

fun AudioFormat.normalize() = AudioFormat(
    NormalizedSampleRate,
    NormalizedSampleSizeInBits,
    channels,
    true,
    isBigEndian,
)

const val NormalizedSampleRate = 44100f
const val NormalizedSampleSizeInBits = 16
