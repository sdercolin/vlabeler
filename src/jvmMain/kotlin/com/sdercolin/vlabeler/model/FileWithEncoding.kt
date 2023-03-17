package com.sdercolin.vlabeler.model

import kotlinx.serialization.Serializable

/**
 * A serializable data to represent a file with its encoding.
 */
@Serializable
data class FileWithEncoding(
    val file: String? = null,
    val encoding: String? = null,
)
