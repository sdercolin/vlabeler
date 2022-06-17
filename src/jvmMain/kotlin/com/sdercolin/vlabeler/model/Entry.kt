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
    val end: Float,
    val points: List<Float>
)
