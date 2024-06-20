package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.io.Fundamental
import com.sdercolin.vlabeler.io.Power
import com.sdercolin.vlabeler.io.Spectrogram
import com.sdercolin.vlabeler.io.Wave

/**
 * A data chunk of a sample file. Since a sample file can be very large, it is split into chunks to avoid memory
 * overflow.
 *
 * @property info The information of the sample file.
 * @property index The index of the chunk.
 * @property wave The wave data of the chunk.
 * @property spectrogram The spectrogram data of the chunk.
 */
@Immutable
class SampleChunk(
    val info: SampleInfo,
    val index: Int,
    val wave: Wave,
    val spectrogram: Spectrogram?,
    val power: Power?,
    val fundamental: Fundamental?,
)
