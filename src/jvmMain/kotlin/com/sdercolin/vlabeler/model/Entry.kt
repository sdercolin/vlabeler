package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable

/**
 * In millisecond
 */
@Immutable
data class Entry(
    val name: String,
    val start: Float,
    val end: Float,
    val points: List<Float>
)