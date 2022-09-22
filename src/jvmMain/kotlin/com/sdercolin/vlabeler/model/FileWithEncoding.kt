package com.sdercolin.vlabeler.model

import kotlinx.serialization.Serializable

@Serializable
data class FileWithEncoding(
    val file: String? = null,
    val encoding: String? = null,
)
