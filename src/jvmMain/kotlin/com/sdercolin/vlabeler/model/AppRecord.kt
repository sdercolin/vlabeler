package com.sdercolin.vlabeler.model

import kotlinx.serialization.Serializable

@Serializable
data class AppRecord(
    val recentProjects: List<String> = listOf()
) {
    fun addRecent(path: String) = copy(
        recentProjects = (listOf(path) + recentProjects).distinct().take(MaxRecentProjectCount)
    )
}

private const val MaxRecentProjectCount = 10
