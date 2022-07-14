package com.sdercolin.vlabeler.io

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
class Wave(val channels: List<Channel>) {
    @Stable
    class Channel(val data: FloatArray)

    val length get() = channels[0].data.size
}
