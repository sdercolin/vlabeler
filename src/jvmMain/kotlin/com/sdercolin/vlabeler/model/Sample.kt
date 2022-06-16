package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Stable
import com.sdercolin.vlabeler.io.Spectrogram
import com.sdercolin.vlabeler.io.Wave
import java.io.File

@Stable
class Sample(
    val info: SampleInfo,
    val wave: Wave,
    val spectrogram: Spectrogram?
)

data class SampleInfo(
    val name: String,
    val file: File,
    val sampleRate: Float,
    val bitDepth: Int,
    val isFloat: Boolean,
    val channels: Int
)