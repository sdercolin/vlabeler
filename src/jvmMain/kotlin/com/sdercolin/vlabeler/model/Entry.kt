package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * In millisecond
 */
@Serializable
@Immutable
data class Entry(
    val name: String,
    val start: Float,
    val end: Float, // minus or zero value represents a relative value to the sample file's end
    val points: List<Float>
)
