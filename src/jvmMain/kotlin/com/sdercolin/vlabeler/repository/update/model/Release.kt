package com.sdercolin.vlabeler.repository.update.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub response for a release.
 */
@Serializable
class Release(
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("tag_name") val tagName: String,
    @SerialName("published_at") val publishedAt: String,
    val prerelease: Boolean,
    val draft: Boolean,
    val assets: List<Asset>,
)
