package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.io.Spectrogram
import com.sdercolin.vlabeler.io.Wave

@Immutable
class SampleChunk(
    val info: SampleInfo,
    val index: Int,
    val wave: Wave,
    val spectrogram: Spectrogram?,
)
