package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.util.asPathRelativeToHome
import com.sdercolin.vlabeler.util.asSimplifiedPaths
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class AppRecord(
    val recentProjects: List<String> = listOf(),
    val windowSizeDp: Pair<Float, Float> = Pair(1200f, 800f),
    val isPropertyViewDisplayed: Boolean = false,
    val isEntryListPinned: Boolean = false,
    val isToolboxDisplayed: Boolean = false,
    val sampleDirectory: String? = null,
    val workingDirectory: String? = null,
    val labelerName: String? = null
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
