package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.util.asPathRelativeToHome
import com.sdercolin.vlabeler.util.asSimplifiedPaths
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class AppRecord(
    val recentProjects: List<String> = listOf(),
    val windowSizeDp: Pair<Float, Float> = Pair(1200f, 850f),
    val pluginDialogSizeDp: Pair<Float, Float> = Pair(900f, 700f),
    val isPropertyViewDisplayed: Boolean = false,
    val isEntryListPinned: Boolean = false,
    val isToolboxDisplayed: Boolean = false,
    val sampleDirectory: String? = null,
    val workingDirectory: String? = null,
    val labelerName: String? = null,
    val disabledLabelerNames: Set<String> = setOf(),
    val disabledPluginNames: Set<String> = setOf()
) {
    val recentProjectPathsWithDisplayNames
        get() = recentProjects.zip(
            recentProjects.map { it.asPathRelativeToHome() }.asSimplifiedPaths()
        )

    fun addRecent(path: String) = copy(
        recentProjects = (listOf(path) + recentProjects).distinct().take(MaxRecentProjectCount)
    )

    fun setLabelerDisabled(name: String, disabled: Boolean) = copy(
        disabledLabelerNames = if (disabled) disabledLabelerNames + name else disabledLabelerNames - name
    )

    fun setPluginDisabled(name: String, disabled: Boolean) = copy(
        disabledPluginNames = if (disabled) disabledPluginNames + name else disabledPluginNames - name
    )
}

private const val MaxRecentProjectCount = 10
