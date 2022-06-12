package com.sdercolin.vlabeler.model

import com.sdercolin.vlabeler.io.Wave
import java.io.File

class Sample(
    val info: SampleInfo,
    val wave: Wave,
)

data class SampleInfo(
    val name: String,
    val file: File,
    val sampleRate: Float,
    val channels: Int
)