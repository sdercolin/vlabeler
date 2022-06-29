package com.sdercolin.vlabeler.model

import com.sdercolin.vlabeler.util.asPathRelativeToHome
import com.sdercolin.vlabeler.util.asSimplifiedPaths
import kotlinx.serialization.Serializable

@Serializable
data class AppRecord(
    val recentProjects: List<String> = listOf(),
    val isMarkerDisplayed: Boolean = true
) {
    val recentProjectPathsWithDisplayNames
        get() = recentProjects.zip(
            recentProjects.map { it.asPathRelativeToHome() }.asSimplifiedPaths()
        )

    fun addRecent(path: String) = copy(
        recentProjects = (listOf(path) + recentProjects).distinct().take(MaxRecentProjectCount)
    )
}

private const val MaxRecentProjectCount = 10
