package com.sdercolin.vlabeler.tracking.event

import kotlinx.serialization.Serializable

@Serializable
data class BasePluginBox(
    val name: String,
    val version: Int,
)
