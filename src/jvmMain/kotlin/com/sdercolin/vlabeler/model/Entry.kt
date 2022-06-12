package com.sdercolin.vlabeler.model

/**
 * In millisecond
 */
data class Entry(
    val name: String,
    val start: Float,
    val end: Float,
    val points: List<Float>
)