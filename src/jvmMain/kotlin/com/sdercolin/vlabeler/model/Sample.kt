package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.io.Spectrogram
import com.sdercolin.vlabeler.io.Wave
import kotlinx.serialization.Serializable

@Immutable
class Sample(
    val info: SampleInfo,
    val wave: Wave,
    val spectrogram: Spectrogram?
)

@Serializable
@Immutable
data class SampleInfo(
    val name: String,
    val file: String,
    val sampleRate: Float,
    val bitDepth: Int,
    val isFloat: Boolean,
    val channels: Int,
    val length: Int,
    val lengthMillis: Float,
    val maxChunkSize: Int,
    val chunkCount: Int,
    val hasSpectrogram: Boolean,
    val lastModified: Long,
    val algorithmVersion: Int
)
