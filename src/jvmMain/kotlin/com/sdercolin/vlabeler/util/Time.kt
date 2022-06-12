package com.sdercolin.vlabeler.util

fun toFrame(millis: Float, sampleRate: Float) = millis * sampleRate / 1000
fun toMillisecond(frame: Float, sampleRate: Float) = 1000 * frame / sampleRate